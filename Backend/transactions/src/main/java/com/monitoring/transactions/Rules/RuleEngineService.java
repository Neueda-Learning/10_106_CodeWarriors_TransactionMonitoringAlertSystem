package com.monitoring.transactions.Rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.monitoring.transactions.BankTransactions.BankTransactions;
import com.monitoring.transactions.BankTransactions.BankTransactionsRepository;
import org.springframework.stereotype.Service;

@Service
public class RuleEngineService {

    private final RulesRepository rulesRepository;
    private final BankTransactionsRepository bankTransactionsRepository;

    public RuleEngineService(RulesRepository rulesRepository, BankTransactionsRepository bankTransactionsRepository) {
        this.rulesRepository = rulesRepository;
        this.bankTransactionsRepository = bankTransactionsRepository;
    }

    public List<Rules> evaluateIncomingTransaction(BankTransactions transaction) {
        List<Rules> activeRules = getActiveRules();
        List<BankTransactions> history = bankTransactionsRepository.findAll();
        List<Rules> triggered = new ArrayList<>();

        for (Rules rule : activeRules) {
            if (matchesIncoming(transaction, rule, history)) {
                triggered.add(rule);
            }
        }

        return triggered;
    }

    public boolean matchesPersistedTransaction(BankTransactions transaction, Rules rule, List<BankTransactions> history) {
        if (transaction == null || rule == null || history == null) {
            return false;
        }

        String type = normalize(rule.getType());
        if (type == null) {
            return false;
        }

        switch (type) {
            case "AMOUNT_THRESHOLD":
                return rule.getThreshold() != null
                    && transaction.getAmount() != null
                    && transaction.getAmount().compareTo(rule.getThreshold()) > 0;
            case "DAILY_LIMIT":
                return isDailyLimitBreach(transaction, rule, history, true);
            case "VELOCITY":
                return isVelocityBreach(transaction, rule, history, true);
            case "NEW_PAYEE":
                return isNewPayee(transaction, history, true);
            default:
                return false;
        }
    }

    public String buildAlertReason(Rules rule) {
        String type = normalize(rule.getType());
        if ("AMOUNT_THRESHOLD".equals(type) && rule.getThreshold() != null) {
            return "Transaction amount exceeded configured threshold of " + rule.getThreshold() + ".";
        }
        if ("DAILY_LIMIT".equals(type) && rule.getThreshold() != null) {
            return "Daily outbound transaction total exceeded configured limit of " + rule.getThreshold() + ".";
        }
        if ("VELOCITY".equals(type) && rule.getMaxTransactions() != null && rule.getTimeWindow() != null) {
            return "Transaction velocity exceeded " + rule.getMaxTransactions()
                + " transactions within " + rule.getTimeWindow() + " minutes.";
        }
        if ("NEW_PAYEE".equals(type)) {
            return "Transaction sent to a new payee/counterparty for this source account.";
        }
        return "Transaction violated monitoring rule: " + (rule.getName() == null ? "Unnamed Rule" : rule.getName()) + ".";
    }

    private List<Rules> getActiveRules() {
        List<Rules> allRules = rulesRepository.findAll();
        List<Rules> active = new ArrayList<>();
        for (Rules rule : allRules) {
            if (Boolean.TRUE.equals(rule.getActive())) {
                active.add(rule);
            }
        }
        return active;
    }

    private boolean matchesIncoming(BankTransactions transaction, Rules rule, List<BankTransactions> history) {
        if (transaction == null || rule == null || history == null) {
            return false;
        }

        String type = normalize(rule.getType());
        if (type == null) {
            return false;
        }

        switch (type) {
            case "AMOUNT_THRESHOLD":
                return rule.getThreshold() != null
                    && transaction.getAmount() != null
                    && transaction.getAmount().compareTo(rule.getThreshold()) > 0;
            case "DAILY_LIMIT":
                return isDailyLimitBreach(transaction, rule, history, false);
            case "VELOCITY":
                return isVelocityBreach(transaction, rule, history, false);
            case "NEW_PAYEE":
                return isNewPayee(transaction, history, false);
            default:
                return false;
        }
    }

    private boolean isDailyLimitBreach(BankTransactions tx, Rules rule, List<BankTransactions> history, boolean persisted) {
        if (rule.getThreshold() == null || tx.getAmount() == null || tx.getTransactionTime() == null || tx.getFromAccountId() == null) {
            return false;
        }

        LocalDate txDate = tx.getTransactionTime().toLocalDate();
        BigDecimal runningTotal = BigDecimal.ZERO;

        for (BankTransactions item : history) {
            if (!Objects.equals(item.getFromAccountId(), tx.getFromAccountId())) {
                continue;
            }
            if (item.getTransactionTime() == null || item.getAmount() == null) {
                continue;
            }
            if (!item.getTransactionTime().toLocalDate().equals(txDate)) {
                continue;
            }
            if (isEarlierOrSameEvent(item, tx, persisted)) {
                runningTotal = runningTotal.add(item.getAmount());
            }
        }

        if (!persisted) {
            runningTotal = runningTotal.add(tx.getAmount());
        }

        return runningTotal.compareTo(rule.getThreshold()) > 0;
    }

    private boolean isVelocityBreach(BankTransactions tx, Rules rule, List<BankTransactions> history, boolean persisted) {
        if (rule.getTimeWindow() == null || rule.getMaxTransactions() == null || tx.getTransactionTime() == null || tx.getFromAccountId() == null) {
            return false;
        }

        int count = 0;
        for (BankTransactions item : history) {
            if (!Objects.equals(item.getFromAccountId(), tx.getFromAccountId())) {
                continue;
            }
            if (item.getTransactionTime() == null) {
                continue;
            }
            if (item.getTransactionTime().isBefore(tx.getTransactionTime().minusMinutes(rule.getTimeWindow()))) {
                continue;
            }
            if (!isEarlierOrSameEvent(item, tx, persisted)) {
                continue;
            }
            count++;
        }

        if (!persisted) {
            count++;
        }

        return count > rule.getMaxTransactions();
    }

    private boolean isNewPayee(BankTransactions tx, List<BankTransactions> history, boolean persisted) {
        if (tx.getFromAccountId() == null || tx.getToAccountId() == null || tx.getTransactionTime() == null) {
            return false;
        }

        List<BankTransactions> samePair = history.stream()
            .filter(item -> Objects.equals(item.getFromAccountId(), tx.getFromAccountId()))
            .filter(item -> Objects.equals(item.getToAccountId(), tx.getToAccountId()))
            .filter(item -> item.getTransactionTime() != null)
            .toList();

        if (!persisted) {
            return samePair.stream().noneMatch(item -> item.getTransactionTime().isBefore(tx.getTransactionTime()));
        }

        BankTransactions earliest = samePair.stream()
            .min(Comparator
                .comparing(BankTransactions::getTransactionTime)
                .thenComparing(BankTransactions::getId, Comparator.nullsLast(Long::compareTo)))
            .orElse(null);

        return earliest != null && Objects.equals(earliest.getId(), tx.getId());
    }

    private boolean isEarlierOrSameEvent(BankTransactions item, BankTransactions anchor, boolean persisted) {
        if (item.getTransactionTime().isBefore(anchor.getTransactionTime())) {
            return true;
        }
        if (item.getTransactionTime().isAfter(anchor.getTransactionTime())) {
            return false;
        }
        if (!persisted) {
            return true;
        }
        if (item.getId() == null || anchor.getId() == null) {
            return true;
        }
        return item.getId() <= anchor.getId();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase(Locale.ROOT);
    }
}

