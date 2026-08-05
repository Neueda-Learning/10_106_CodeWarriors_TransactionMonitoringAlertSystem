document.addEventListener('DOMContentLoaded', () => {
    const rulesTableBody = document.getElementById('rules-table-body');
    if (!rulesTableBody) {
        return;
    }

    const ui = {
        statTotalRules: document.getElementById('stat-total-rules'),
        statActiveRules: document.getElementById('stat-active-rules'),
        statDisabledRules: document.getElementById('stat-disabled-rules'),
        statRuleTypes: document.getElementById('stat-rule-types'),
        statCards: Array.from(document.querySelectorAll('.rules-stat-card')),

        searchInput: document.getElementById('rule-search'),
        typeFilter: document.getElementById('rule-type-filter'),
        severityFilter: document.getElementById('rule-severity-filter'),
        statusFilter: document.getElementById('rule-status-filter'),
        resetFiltersBtn: document.getElementById('reset-rule-filters'),

        addRuleBtn: document.getElementById('add-rule-btn'),
        ruleForm: document.getElementById('rule-form'),
        ruleModalEl: document.getElementById('ruleModal'),
        ruleModalTitle: document.getElementById('ruleModalLabel'),
        saveRuleBtn: document.getElementById('save-rule-btn'),

        ruleIdField: document.getElementById('rule-id-field'),
        ruleNameField: document.getElementById('rule-name-field'),
        ruleTypeField: document.getElementById('rule-type-field'),
        ruleSeverityField: document.getElementById('rule-severity-field'),
        ruleStatusField: document.getElementById('rule-status-field'),
        ruleDescriptionField: document.getElementById('rule-description-field'),
        dynamicFieldsHost: document.getElementById('rule-type-dynamic-fields'),

        ruleViewModalEl: document.getElementById('ruleViewModal'),
        ruleViewContent: document.getElementById('rule-view-content')
    };

    const ruleModal = bootstrap.Modal.getOrCreateInstance(ui.ruleModalEl);
    const ruleViewModal = bootstrap.Modal.getOrCreateInstance(ui.ruleViewModalEl);

    const state = {
        nextRuleId: 105,
        editingRuleId: null,
        rules: [
            {
                id: 101,
                name: 'Amount Threshold',
                type: 'Amount Threshold',
                severity: 'HIGH',
                status: 'ACTIVE',
                alertsGenerated: 41,
                description: 'Triggers when a single transaction exceeds the configured amount threshold.',
                lastUpdated: '2026-07-28 10:40',
                config: {
                    thresholdAmount: 10000
                }
            },
            {
                id: 102,
                name: 'Velocity Rule',
                type: 'Velocity Rule',
                severity: 'CRITICAL',
                status: 'ACTIVE',
                alertsGenerated: 63,
                description: 'Detects rapid transaction bursts over a short period.',
                lastUpdated: '2026-08-01 09:10',
                config: {
                    maxTransactions: 6,
                    timeWindowMinutes: 10
                }
            },
            {
                id: 103,
                name: 'New Payee Check',
                type: 'New Payee Check',
                severity: 'MEDIUM',
                status: 'INACTIVE',
                alertsGenerated: 18,
                description: 'Flags higher risk first-time beneficiary transfers.',
                lastUpdated: '2026-07-25 15:05',
                config: {
                    enabled: true
                }
            },
            {
                id: 104,
                name: 'Daily Limit',
                type: 'Daily Limit',
                severity: 'HIGH',
                status: 'ACTIVE',
                alertsGenerated: 29,
                description: 'Monitors daily outgoing amount and detects limit breaches.',
                lastUpdated: '2026-08-03 12:22',
                config: {
                    dailyLimitAmount: 25000
                }
            }
        ]
    };

    function formatNow() {
        const d = new Date();
        const yyyy = d.getFullYear();
        const mm = String(d.getMonth() + 1).padStart(2, '0');
        const dd = String(d.getDate()).padStart(2, '0');
        const hh = String(d.getHours()).padStart(2, '0');
        const mi = String(d.getMinutes()).padStart(2, '0');
        return `${yyyy}-${mm}-${dd} ${hh}:${mi}`;
    }

    function currency(value) {
        return `$${Number(value).toLocaleString(undefined, { maximumFractionDigits: 2 })}`;
    }

    function getSeverityBadgeClass(severity) {
        if (severity === 'CRITICAL') return 'text-bg-danger';
        if (severity === 'HIGH') return 'text-bg-warning';
        if (severity === 'MEDIUM') return 'text-bg-info';
        return 'text-bg-secondary';
    }

    function getStatusBadgeClass(status) {
        return status === 'ACTIVE' ? 'text-bg-success' : 'text-bg-secondary';
    }

    function formatRuleLimit(rule) {
        if (rule.type === 'Amount Threshold') {
            return `Threshold: ${currency(rule.config.thresholdAmount)}`;
        }
        if (rule.type === 'Velocity Rule') {
            return `${rule.config.maxTransactions} tx / ${rule.config.timeWindowMinutes} min`;
        }
        if (rule.type === 'New Payee Check') {
            return `Enabled: ${rule.config.enabled ? 'Yes' : 'No'}`;
        }
        if (rule.type === 'Daily Limit') {
            return `Daily Cap: ${currency(rule.config.dailyLimitAmount)}`;
        }
        return '-';
    }

    function renderDynamicFields(ruleType, config = null) {
        const source = config || {};
        let html = '';

        if (ruleType === 'Amount Threshold') {
            html = `
                <div class="row g-3">
                    <div class="col-md-6">
                        <label for="field-threshold-amount" class="form-label">Threshold Amount</label>
                        <input id="field-threshold-amount" type="number" min="0" step="0.01" class="form-control" value="${source.thresholdAmount ?? ''}" required>
                    </div>
                </div>
            `;
        } else if (ruleType === 'Velocity Rule') {
            html = `
                <div class="row g-3">
                    <div class="col-md-6">
                        <label for="field-max-transactions" class="form-label">Maximum Transactions</label>
                        <input id="field-max-transactions" type="number" min="1" step="1" class="form-control" value="${source.maxTransactions ?? ''}" required>
                    </div>
                    <div class="col-md-6">
                        <label for="field-time-window" class="form-label">Time Window (minutes)</label>
                        <input id="field-time-window" type="number" min="1" step="1" class="form-control" value="${source.timeWindowMinutes ?? ''}" required>
                    </div>
                </div>
            `;
        } else if (ruleType === 'New Payee Check') {
            html = `
                <div class="form-check form-switch mt-2">
                    <input class="form-check-input" type="checkbox" role="switch" id="field-new-payee-enabled" ${source.enabled !== false ? 'checked' : ''}>
                    <label class="form-check-label" for="field-new-payee-enabled">Enabled</label>
                </div>
            `;
        } else if (ruleType === 'Daily Limit') {
            html = `
                <div class="row g-3">
                    <div class="col-md-6">
                        <label for="field-daily-limit" class="form-label">Daily Limit Amount</label>
                        <input id="field-daily-limit" type="number" min="0" step="0.01" class="form-control" value="${source.dailyLimitAmount ?? ''}" required>
                    </div>
                </div>
            `;
        }

        ui.dynamicFieldsHost.innerHTML = html;
    }

    function readDynamicFields(ruleType) {
        if (ruleType === 'Amount Threshold') {
            const thresholdAmount = Number(document.getElementById('field-threshold-amount').value);
            return { thresholdAmount };
        }
        if (ruleType === 'Velocity Rule') {
            const maxTransactions = Number(document.getElementById('field-max-transactions').value);
            const timeWindowMinutes = Number(document.getElementById('field-time-window').value);
            return { maxTransactions, timeWindowMinutes };
        }
        if (ruleType === 'New Payee Check') {
            const enabled = document.getElementById('field-new-payee-enabled').checked;
            return { enabled };
        }
        if (ruleType === 'Daily Limit') {
            const dailyLimitAmount = Number(document.getElementById('field-daily-limit').value);
            return { dailyLimitAmount };
        }
        return {};
    }

    function validateDynamicFields(ruleType, config) {
        if (ruleType === 'Amount Threshold') {
            return Number.isFinite(config.thresholdAmount) && config.thresholdAmount > 0;
        }
        if (ruleType === 'Velocity Rule') {
            return Number.isInteger(config.maxTransactions) &&
                config.maxTransactions > 0 &&
                Number.isInteger(config.timeWindowMinutes) &&
                config.timeWindowMinutes > 0;
        }
        if (ruleType === 'New Payee Check') {
            return typeof config.enabled === 'boolean';
        }
        if (ruleType === 'Daily Limit') {
            return Number.isFinite(config.dailyLimitAmount) && config.dailyLimitAmount > 0;
        }
        return false;
    }

    function getFilters() {
        return {
            searchTerm: ui.searchInput.value.trim().toLowerCase(),
            type: ui.typeFilter.value,
            severity: ui.severityFilter.value,
            status: ui.statusFilter.value
        };
    }

    function applyFilters(rules, filters) {
        return rules.filter(rule => {
            const matchesSearch = String(rule.id).includes(filters.searchTerm) ||
                rule.name.toLowerCase().includes(filters.searchTerm) ||
                formatRuleLimit(rule).toLowerCase().includes(filters.searchTerm);
            const matchesType = filters.type === 'ALL' || rule.type === filters.type;
            const matchesSeverity = filters.severity === 'ALL' || rule.severity === filters.severity;
            const matchesStatus = filters.status === 'ALL' || rule.status === filters.status;
            return matchesSearch && matchesType && matchesSeverity && matchesStatus;
        });
    }

    function resetAllFilters() {
        ui.searchInput.value = '';
        ui.typeFilter.value = 'ALL';
        ui.severityFilter.value = 'ALL';
        ui.statusFilter.value = 'ALL';
    }

    function renderStats() {
        const totalRules = state.rules.length;
        const activeRules = state.rules.filter(rule => rule.status === 'ACTIVE').length;
        const disabledRules = totalRules - activeRules;
        const ruleTypes = new Set(state.rules.map(rule => rule.type)).size;

        ui.statTotalRules.textContent = totalRules;
        ui.statActiveRules.textContent = activeRules;
        ui.statDisabledRules.textContent = disabledRules;
        ui.statRuleTypes.textContent = ruleTypes;
    }

    function renderTable() {
        const filteredRules = applyFilters(state.rules, getFilters());
        rulesTableBody.innerHTML = '';

        if (filteredRules.length === 0) {
            rulesTableBody.innerHTML = '<tr><td colspan="9" class="text-center text-muted py-4">No rules found for the selected filters.</td></tr>';
            return;
        }

        filteredRules.forEach(rule => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="fw-bold">R-${rule.id}</td>
                <td>${rule.name}</td>
                <td>${rule.type}</td>
                <td>${formatRuleLimit(rule)}</td>
                <td><span class="badge ${getSeverityBadgeClass(rule.severity)}">${rule.severity}</span></td>
                <td><span class="badge ${getStatusBadgeClass(rule.status)}">${rule.status}</span></td>
                <td>${rule.alertsGenerated}</td>
                <td><small class="text-muted">${rule.lastUpdated}</small></td>
                <td class="text-end">
                    <div class="btn-group btn-group-sm rules-action-group" role="group">
                        <button class="btn btn-outline-primary" data-action="view" data-id="${rule.id}">View</button>
                        <button class="btn btn-outline-secondary" data-action="edit" data-id="${rule.id}">Edit</button>
                        <button class="btn btn-outline-danger" data-action="delete" data-id="${rule.id}">Delete</button>
                        <button class="btn btn-outline-info" data-action="alerts" data-id="${rule.id}">View Alerts</button>
                        <button class="btn btn-outline-dark" data-action="toggle" data-id="${rule.id}">${rule.status === 'ACTIVE' ? 'Disable' : 'Enable'}</button>
                    </div>
                </td>
            `;
            rulesTableBody.appendChild(tr);
        });
    }

    function renderRuleView(rule) {
        ui.ruleViewContent.innerHTML = `
            <div><span class="text-muted">Rule ID</span><h6 class="mb-3">R-${rule.id}</h6></div>
            <div><span class="text-muted">Rule Name</span><h6 class="mb-3">${rule.name}</h6></div>
            <div><span class="text-muted">Rule Type</span><h6 class="mb-3">${rule.type}</h6></div>
            <div><span class="text-muted">Threshold / Limit</span><h6 class="mb-3">${formatRuleLimit(rule)}</h6></div>
            <div><span class="text-muted">Severity</span><h6 class="mb-3"><span class="badge ${getSeverityBadgeClass(rule.severity)}">${rule.severity}</span></h6></div>
            <div><span class="text-muted">Status</span><h6 class="mb-3"><span class="badge ${getStatusBadgeClass(rule.status)}">${rule.status}</span></h6></div>
            <div><span class="text-muted">Alerts Generated</span><h6 class="mb-3">${rule.alertsGenerated}</h6></div>
            <div><span class="text-muted">Last Updated</span><h6 class="mb-3">${rule.lastUpdated}</h6></div>
            <div class="rules-view-description"><span class="text-muted">Description</span><p class="mb-0 mt-1">${rule.description || 'No description provided.'}</p></div>
        `;
    }

    function renderAll() {
        renderStats();
        renderTable();
    }

    const RulesStore = {
        create(ruleInput) {
            const rule = {
                id: state.nextRuleId++,
                ...ruleInput,
                alertsGenerated: 0,
                lastUpdated: formatNow()
            };
            state.rules.push(rule);
            return rule;
        },
        update(ruleId, updates) {
            const target = state.rules.find(rule => rule.id === ruleId);
            if (!target) return null;
            Object.assign(target, updates, { lastUpdated: formatNow() });
            return target;
        },
        delete(ruleId) {
            const idx = state.rules.findIndex(rule => rule.id === ruleId);
            if (idx < 0) return false;
            state.rules.splice(idx, 1);
            return true;
        },
        toggleStatus(ruleId) {
            const target = state.rules.find(rule => rule.id === ruleId);
            if (!target) return null;
            target.status = target.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
            target.lastUpdated = formatNow();
            return target;
        },
        getById(ruleId) {
            return state.rules.find(rule => rule.id === ruleId) || null;
        }
    };

    function resetForm() {
        state.editingRuleId = null;
        ui.ruleForm.reset();
        ui.ruleIdField.value = '';
        ui.ruleModalTitle.textContent = 'Add Rule';
        ui.saveRuleBtn.textContent = 'Save';
        ui.ruleSeverityField.value = 'MEDIUM';
        ui.ruleStatusField.value = 'ACTIVE';
        ui.ruleTypeField.value = 'Amount Threshold';
        renderDynamicFields('Amount Threshold');
    }

    function openEditForm(rule) {
        state.editingRuleId = rule.id;
        ui.ruleIdField.value = String(rule.id);
        ui.ruleNameField.value = rule.name;
        ui.ruleTypeField.value = rule.type;
        ui.ruleSeverityField.value = rule.severity;
        ui.ruleStatusField.value = rule.status;
        ui.ruleDescriptionField.value = rule.description || '';
        ui.ruleModalTitle.textContent = 'Edit Rule';
        ui.saveRuleBtn.textContent = 'Update';
        renderDynamicFields(rule.type, rule.config);
        ruleModal.show();
    }

    function collectFormData() {
        const name = ui.ruleNameField.value.trim();
        const type = ui.ruleTypeField.value;
        const severity = ui.ruleSeverityField.value;
        const status = ui.ruleStatusField.value;
        const description = ui.ruleDescriptionField.value.trim();
        const config = readDynamicFields(type);

        if (!name || !type || !validateDynamicFields(type, config)) {
            return null;
        }

        return { name, type, severity, status, description, config };
    }

    ui.statCards.forEach(card => {
        card.addEventListener('click', () => {
            const action = card.getAttribute('data-stat-filter');
            if (action === 'RESET') {
                resetAllFilters();
                renderTable();
                return;
            }
            if (action === 'ALL') {
                ui.statusFilter.value = 'ALL';
            } else {
                ui.statusFilter.value = action;
            }
            renderTable();
        });
    });

    ui.addRuleBtn.addEventListener('click', () => {
        resetForm();
        ruleModal.show();
    });

    ui.ruleTypeField.addEventListener('change', event => {
        renderDynamicFields(event.target.value);
    });

    ui.ruleForm.addEventListener('submit', event => {
        event.preventDefault();

        if (!ui.ruleForm.checkValidity()) {
            ui.ruleForm.reportValidity();
            return;
        }

        const payload = collectFormData();
        if (!payload) {
            window.alert('Please complete all required rule fields.');
            return;
        }

        if (state.editingRuleId === null) {
            RulesStore.create(payload);
        } else {
            RulesStore.update(state.editingRuleId, payload);
        }

        ruleModal.hide();
        renderAll();
    });

    ui.resetFiltersBtn.addEventListener('click', () => {
        resetAllFilters();
        renderTable();
    });

    ui.searchInput.addEventListener('input', renderTable);
    ui.typeFilter.addEventListener('change', renderTable);
    ui.severityFilter.addEventListener('change', renderTable);
    ui.statusFilter.addEventListener('change', renderTable);

    rulesTableBody.addEventListener('click', event => {
        const actionButton = event.target.closest('button[data-action]');
        if (!actionButton) return;

        const ruleId = Number(actionButton.getAttribute('data-id'));
        const action = actionButton.getAttribute('data-action');
        const rule = RulesStore.getById(ruleId);
        if (!rule) return;

        if (action === 'view') {
            renderRuleView(rule);
            ruleViewModal.show();
            return;
        }

        if (action === 'edit') {
            openEditForm(rule);
            return;
        }

        if (action === 'delete') {
            const approved = window.confirm(`Delete rule R-${rule.id} (${rule.name})?`);
            if (!approved) return;
            RulesStore.delete(rule.id);
            renderAll();
            return;
        }

        if (action === 'alerts') {
            window.location.href = `alerts.html?ruleId=${rule.id}`;
            return;
        }

        if (action === 'toggle') {
            RulesStore.toggleStatus(rule.id);
            renderAll();
        }
    });

    ui.ruleModalEl.addEventListener('hidden.bs.modal', resetForm);

    resetForm();
    renderAll();
});
