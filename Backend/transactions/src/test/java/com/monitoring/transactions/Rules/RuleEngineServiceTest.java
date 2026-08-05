package com.monitoring.transactions.Rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.monitoring.transactions.BankTransactions.BankTransactions;
import com.monitoring.transactions.BankTransactions.BankTransactionsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RuleEngineServiceTest {

	@Mock
	private RulesRepository rulesRepository;

	@Mock
	private BankTransactionsRepository bankTransactionsRepository;

	private RuleEngineService ruleEngineService;

	@BeforeEach
	void setUp() {
		ruleEngineService = new RuleEngineService(rulesRepository, bankTransactionsRepository);
	}

	private BankTransactions createTransaction(Long id, Long from, Long to, BigDecimal amount, LocalDateTime time) {
		BankTransactions tx = new BankTransactions();
		tx.setId(id);
		tx.setFromAccountId(from);
		tx.setToAccountId(to);
		tx.setAmount(amount);
		tx.setTransactionTime(time);
		return tx;
	}

	@Test
	void testEvaluateIncomingTransaction_AmountThreshold_Triggers() {
		Rules rule = new Rules();
		rule.setId(1L);
		rule.setType("AMOUNT_THRESHOLD");
		rule.setThreshold(new BigDecimal("10000.00"));
		rule.setActive(true);

		when(rulesRepository.findAll()).thenReturn(List.of(rule));
		when(bankTransactionsRepository.findAll()).thenReturn(Collections.emptyList());

		BankTransactions incoming = createTransaction(null, 1L, 2L, new BigDecimal("15000.00"), LocalDateTime.now());
		List<Rules> triggered = ruleEngineService.evaluateIncomingTransaction(incoming);

		assertEquals(1, triggered.size());
		assertEquals(1L, triggered.get(0).getId());
	}

	@Test
	void testEvaluateIncomingTransaction_AmountThreshold_DoesNotTrigger() {
		Rules rule = new Rules();
		rule.setId(1L);
		rule.setType("AMOUNT_THRESHOLD");
		rule.setThreshold(new BigDecimal("10000.00"));
		rule.setActive(true);

		when(rulesRepository.findAll()).thenReturn(List.of(rule));
		when(bankTransactionsRepository.findAll()).thenReturn(Collections.emptyList());

		BankTransactions incoming = createTransaction(null, 1L, 2L, new BigDecimal("5000.00"), LocalDateTime.now());
		List<Rules> triggered = ruleEngineService.evaluateIncomingTransaction(incoming);

		assertTrue(triggered.isEmpty());
	}

	@Test
	void testDailyLimitBreach_Triggers() {
		Rules rule = new Rules();
		rule.setId(2L);
		rule.setType("DAILY_LIMIT");
		rule.setThreshold(new BigDecimal("5000.00"));
		rule.setActive(true);

		when(rulesRepository.findAll()).thenReturn(List.of(rule));
		
		LocalDateTime today = LocalDateTime.now();
		BankTransactions pastTx = createTransaction(1L, 1L, 2L, new BigDecimal("3000.00"), today.minusHours(1));
		when(bankTransactionsRepository.findAll()).thenReturn(List.of(pastTx));

		BankTransactions incoming = createTransaction(null, 1L, 3L, new BigDecimal("2500.00"), today);
		List<Rules> triggered = ruleEngineService.evaluateIncomingTransaction(incoming);

		assertEquals(1, triggered.size());
	}

	@Test
	void testDailyLimitBreach_DifferentDay_DoesNotTrigger() {
		Rules rule = new Rules();
		rule.setId(2L);
		rule.setType("DAILY_LIMIT");
		rule.setThreshold(new BigDecimal("5000.00"));
		rule.setActive(true);

		when(rulesRepository.findAll()).thenReturn(List.of(rule));
		
		LocalDateTime today = LocalDateTime.now();
		BankTransactions pastTx = createTransaction(1L, 1L, 2L, new BigDecimal("3000.00"), today.minusDays(1));
		when(bankTransactionsRepository.findAll()).thenReturn(List.of(pastTx));

		BankTransactions incoming = createTransaction(null, 1L, 3L, new BigDecimal("2500.00"), today);
		List<Rules> triggered = ruleEngineService.evaluateIncomingTransaction(incoming);

		assertTrue(triggered.isEmpty());
	}

	@Test
	void testVelocityBreach_Triggers() {
		Rules rule = new Rules();
		rule.setId(3L);
		rule.setType("VELOCITY");
		rule.setTimeWindow(10);
		rule.setMaxTransactions(2);
		rule.setActive(true);

		when(rulesRepository.findAll()).thenReturn(List.of(rule));
		
		LocalDateTime now = LocalDateTime.now();
		BankTransactions tx1 = createTransaction(1L, 1L, 2L, new BigDecimal("100.00"), now.minusMinutes(5));
		BankTransactions tx2 = createTransaction(2L, 1L, 3L, new BigDecimal("200.00"), now.minusMinutes(2));
		when(bankTransactionsRepository.findAll()).thenReturn(List.of(tx1, tx2));

		BankTransactions incoming = createTransaction(null, 1L, 4L, new BigDecimal("300.00"), now);
		List<Rules> triggered = ruleEngineService.evaluateIncomingTransaction(incoming);

		assertEquals(1, triggered.size());
	}

	@Test
	void testVelocityBreach_OutsideWindow_DoesNotTrigger() {
		Rules rule = new Rules();
		rule.setId(3L);
		rule.setType("VELOCITY");
		rule.setTimeWindow(10);
		rule.setMaxTransactions(2);
		rule.setActive(true);

		when(rulesRepository.findAll()).thenReturn(List.of(rule));
		
		LocalDateTime now = LocalDateTime.now();
		BankTransactions tx1 = createTransaction(1L, 1L, 2L, new BigDecimal("100.00"), now.minusMinutes(15));
		BankTransactions tx2 = createTransaction(2L, 1L, 3L, new BigDecimal("200.00"), now.minusMinutes(2));
		when(bankTransactionsRepository.findAll()).thenReturn(List.of(tx1, tx2));

		BankTransactions incoming = createTransaction(null, 1L, 4L, new BigDecimal("300.00"), now);
		List<Rules> triggered = ruleEngineService.evaluateIncomingTransaction(incoming);

		assertTrue(triggered.isEmpty());
	}

	@Test
	void testNewPayee_Triggers() {
		Rules rule = new Rules();
		rule.setId(4L);
		rule.setType("NEW_PAYEE");
		rule.setActive(true);

		when(rulesRepository.findAll()).thenReturn(List.of(rule));
		
		LocalDateTime now = LocalDateTime.now();
		BankTransactions pastTx = createTransaction(1L, 1L, 2L, new BigDecimal("100.00"), now.minusDays(1));
		when(bankTransactionsRepository.findAll()).thenReturn(List.of(pastTx));

		BankTransactions incoming = createTransaction(null, 1L, 3L, new BigDecimal("500.00"), now);
		List<Rules> triggered = ruleEngineService.evaluateIncomingTransaction(incoming);

		assertEquals(1, triggered.size());
	}

	@Test
	void testNewPayee_ExistingPayee_DoesNotTrigger() {
		Rules rule = new Rules();
		rule.setId(4L);
		rule.setType("NEW_PAYEE");
		rule.setActive(true);

		when(rulesRepository.findAll()).thenReturn(List.of(rule));
		
		LocalDateTime now = LocalDateTime.now();
		BankTransactions pastTx = createTransaction(1L, 1L, 2L, new BigDecimal("100.00"), now.minusDays(1));
		when(bankTransactionsRepository.findAll()).thenReturn(List.of(pastTx));

		BankTransactions incoming = createTransaction(null, 1L, 2L, new BigDecimal("500.00"), now);
		List<Rules> triggered = ruleEngineService.evaluateIncomingTransaction(incoming);

		assertTrue(triggered.isEmpty());
	}

	@Test
	void testEvaluateIncomingTransaction_IgnoresInactiveRules() {
		Rules inactiveRule = new Rules();
		inactiveRule.setId(5L);
		inactiveRule.setType("AMOUNT_THRESHOLD");
		inactiveRule.setThreshold(new BigDecimal("1000.00"));
		inactiveRule.setActive(false);

		when(rulesRepository.findAll()).thenReturn(List.of(inactiveRule));
		when(bankTransactionsRepository.findAll()).thenReturn(Collections.emptyList());

		BankTransactions incoming = createTransaction(null, 1L, 2L, new BigDecimal("5000.00"), LocalDateTime.now());
		List<Rules> triggered = ruleEngineService.evaluateIncomingTransaction(incoming);

		assertTrue(triggered.isEmpty());
	}

	@Test
	void testEvaluateIncomingTransaction_UnknownRuleType_DoesNotTrigger() {
		Rules rule = new Rules();
		rule.setId(6L);
		rule.setType("SOME_UNKNOWN_TYPE");
		rule.setActive(true);

		when(rulesRepository.findAll()).thenReturn(List.of(rule));
		when(bankTransactionsRepository.findAll()).thenReturn(Collections.emptyList());

		BankTransactions incoming = createTransaction(null, 1L, 2L, new BigDecimal("99999.00"), LocalDateTime.now());
		List<Rules> triggered = ruleEngineService.evaluateIncomingTransaction(incoming);

		assertTrue(triggered.isEmpty());
	}

	@Test
	void testEvaluateIncomingTransaction_AmountEqualToThreshold_DoesNotTrigger() {
		Rules rule = new Rules();
		rule.setId(7L);
		rule.setType("AMOUNT_THRESHOLD");
		rule.setThreshold(new BigDecimal("10000.00"));
		rule.setActive(true);

		when(rulesRepository.findAll()).thenReturn(List.of(rule));
		when(bankTransactionsRepository.findAll()).thenReturn(Collections.emptyList());

		BankTransactions incoming = createTransaction(null, 1L, 2L, new BigDecimal("10000.00"), LocalDateTime.now());
		List<Rules> triggered = ruleEngineService.evaluateIncomingTransaction(incoming);

		assertTrue(triggered.isEmpty());
	}

	@Test
	void testBuildAlertReason_FallsBackToGenericReason() {
		Rules rule = new Rules();
		rule.setName("Custom Rule");
		rule.setType("UNKNOWN");

		String reason = ruleEngineService.buildAlertReason(rule);

		assertTrue(reason.contains("Transaction violated monitoring rule"));
		assertTrue(reason.contains("Custom Rule"));
	}
}
