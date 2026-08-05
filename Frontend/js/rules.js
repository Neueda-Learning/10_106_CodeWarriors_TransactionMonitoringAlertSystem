document.addEventListener('DOMContentLoaded', () => {
    const rulesTableBody = document.getElementById('rules-table-body');
    if (!rulesTableBody) {
        return;
    }

    const API_BASE_URL = window.localStorage.getItem('apiBaseUrl') || 'http://localhost:8085';

    const RULE_TYPES = [
        { value: 'AMOUNT_THRESHOLD', label: 'Amount Threshold' },
        { value: 'VELOCITY', label: 'Velocity Rule' },
        { value: 'NEW_PAYEE', label: 'New Payee Check' },
        { value: 'DAILY_LIMIT', label: 'Daily Limit' }
    ];

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
        dynamicFieldsHost: document.getElementById('rule-type-dynamic-fields'),

        ruleViewModalEl: document.getElementById('ruleViewModal'),
        ruleViewContent: document.getElementById('rule-view-content')
    };

    const ruleModal = bootstrap.Modal.getOrCreateInstance(ui.ruleModalEl);
    const ruleViewModal = bootstrap.Modal.getOrCreateInstance(ui.ruleViewModalEl);

    const state = {
        rules: [],
        editingRuleId: null
    };

    function typeLabel(value) {
        const found = RULE_TYPES.find(type => type.value === value);
        return found ? found.label : value;
    }

    function currency(value) {
        const parsed = Number(value);
        if (!Number.isFinite(parsed)) {
            return '-';
        }
        return `$${parsed.toLocaleString(undefined, { maximumFractionDigits: 2 })}`;
    }

    function getSeverityBadgeClass(severity) {
        if (severity === 'CRITICAL') return 'text-bg-danger';
        if (severity === 'HIGH') return 'text-bg-warning';
        if (severity === 'MEDIUM') return 'text-bg-info';
        return 'text-bg-secondary';
    }

    function getStatusBadgeClass(active) {
        return active ? 'text-bg-success' : 'text-bg-secondary';
    }

    function formatRuleLimit(rule) {
        if (rule.type === 'AMOUNT_THRESHOLD' || rule.type === 'DAILY_LIMIT') {
            return rule.threshold != null ? `Threshold: ${currency(rule.threshold)}` : '-';
        }
        if (rule.type === 'VELOCITY') {
            if (rule.maxTransactions != null && rule.timeWindow != null) {
                return `${rule.maxTransactions} tx / ${rule.timeWindow} min`;
            }
            return '-';
        }
        if (rule.type === 'NEW_PAYEE') {
            return 'First transaction to payee';
        }
        return '-';
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
            const ruleStatus = rule.active ? 'ACTIVE' : 'INACTIVE';
            const matchesSearch = String(rule.id).includes(filters.searchTerm) ||
                String(rule.name || '').toLowerCase().includes(filters.searchTerm) ||
                formatRuleLimit(rule).toLowerCase().includes(filters.searchTerm);
            const matchesType = filters.type === 'ALL' || rule.type === filters.type;
            const matchesSeverity = filters.severity === 'ALL' || rule.severity === filters.severity;
            const matchesStatus = filters.status === 'ALL' || ruleStatus === filters.status;
            return matchesSearch && matchesType && matchesSeverity && matchesStatus;
        });
    }

    function renderStats() {
        const totalRules = state.rules.length;
        const activeRules = state.rules.filter(rule => rule.active).length;
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
            const ruleStatus = rule.active ? 'ACTIVE' : 'INACTIVE';
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="fw-bold">R-${rule.id}</td>
                <td>${rule.name || '-'}</td>
                <td>${typeLabel(rule.type)}</td>
                <td>${formatRuleLimit(rule)}</td>
                <td><span class="badge ${getSeverityBadgeClass(rule.severity)}">${rule.severity || '-'}</span></td>
                <td><span class="badge ${getStatusBadgeClass(rule.active)}">${ruleStatus}</span></td>
                <td>-</td>
                <td><small class="text-muted">-</small></td>
                <td class="text-end">
                    <div class="btn-group btn-group-sm rules-action-group" role="group">
                        <button class="btn btn-outline-primary" data-action="view" data-id="${rule.id}">View</button>
                        <button class="btn btn-outline-secondary" data-action="edit" data-id="${rule.id}">Edit</button>
                        <button class="btn btn-outline-danger" data-action="delete" data-id="${rule.id}">Delete</button>
                        <button class="btn btn-outline-dark" data-action="toggle" data-id="${rule.id}">${rule.active ? 'Disable' : 'Enable'}</button>
                    </div>
                </td>
            `;
            rulesTableBody.appendChild(tr);
        });
    }

    function renderRuleView(rule) {
        const ruleStatus = rule.active ? 'ACTIVE' : 'INACTIVE';
        ui.ruleViewContent.innerHTML = `
            <div><span class="text-muted">Rule ID</span><h6 class="mb-3">R-${rule.id}</h6></div>
            <div><span class="text-muted">Rule Name</span><h6 class="mb-3">${rule.name || '-'}</h6></div>
            <div><span class="text-muted">Rule Type</span><h6 class="mb-3">${typeLabel(rule.type)}</h6></div>
            <div><span class="text-muted">Threshold / Limit</span><h6 class="mb-3">${formatRuleLimit(rule)}</h6></div>
            <div><span class="text-muted">Severity</span><h6 class="mb-3"><span class="badge ${getSeverityBadgeClass(rule.severity)}">${rule.severity || '-'}</span></h6></div>
            <div><span class="text-muted">Status</span><h6 class="mb-3"><span class="badge ${getStatusBadgeClass(rule.active)}">${ruleStatus}</span></h6></div>
            <div><span class="text-muted">Alerts Generated</span><h6 class="mb-3">-</h6></div>
            <div><span class="text-muted">Last Updated</span><h6 class="mb-3">-</h6></div>
        `;
    }

    function renderAll() {
        renderStats();
        renderTable();
    }

    function renderTypeSelects() {
        const filterOptions = ['<option value="ALL">All Types</option>']
            .concat(RULE_TYPES.map(type => `<option value="${type.value}">${type.label}</option>`));
        ui.typeFilter.innerHTML = filterOptions.join('');

        const formOptions = ['<option value="">Select type...</option>']
            .concat(RULE_TYPES.map(type => `<option value="${type.value}">${type.label}</option>`));
        ui.ruleTypeField.innerHTML = formOptions.join('');
    }

    function renderDynamicFields(ruleType, source) {
        const config = source || {};
        let html = '';

        if (ruleType === 'AMOUNT_THRESHOLD' || ruleType === 'DAILY_LIMIT') {
            html = `
                <div class="row g-3">
                    <div class="col-md-6">
                        <label for="field-threshold-amount" class="form-label">Threshold Amount</label>
                        <input id="field-threshold-amount" type="number" min="0" step="0.01" class="form-control" value="${config.threshold ?? ''}" required>
                    </div>
                </div>
            `;
        } else if (ruleType === 'VELOCITY') {
            html = `
                <div class="row g-3">
                    <div class="col-md-6">
                        <label for="field-max-transactions" class="form-label">Maximum Transactions</label>
                        <input id="field-max-transactions" type="number" min="1" step="1" class="form-control" value="${config.maxTransactions ?? ''}" required>
                    </div>
                    <div class="col-md-6">
                        <label for="field-time-window" class="form-label">Time Window (minutes)</label>
                        <input id="field-time-window" type="number" min="1" step="1" class="form-control" value="${config.timeWindow ?? ''}" required>
                    </div>
                </div>
            `;
        }

        ui.dynamicFieldsHost.innerHTML = html;
    }

    function readDynamicFields(ruleType) {
        if (ruleType === 'AMOUNT_THRESHOLD' || ruleType === 'DAILY_LIMIT') {
            const threshold = Number(document.getElementById('field-threshold-amount').value);
            return {
                threshold: Number.isFinite(threshold) ? threshold : null,
                timeWindow: null,
                maxTransactions: null
            };
        }
        if (ruleType === 'VELOCITY') {
            const maxTransactions = Number(document.getElementById('field-max-transactions').value);
            const timeWindow = Number(document.getElementById('field-time-window').value);
            return {
                threshold: null,
                timeWindow: Number.isInteger(timeWindow) ? timeWindow : null,
                maxTransactions: Number.isInteger(maxTransactions) ? maxTransactions : null
            };
        }
        return {
            threshold: null,
            timeWindow: null,
            maxTransactions: null
        };
    }

    function validateDynamicFields(ruleType, config) {
        if (ruleType === 'AMOUNT_THRESHOLD' || ruleType === 'DAILY_LIMIT') {
            return Number.isFinite(config.threshold) && config.threshold > 0;
        }
        if (ruleType === 'VELOCITY') {
            return Number.isInteger(config.maxTransactions) && config.maxTransactions > 0 &&
                Number.isInteger(config.timeWindow) && config.timeWindow > 0;
        }
        return true;
    }

    function resetAllFilters() {
        ui.searchInput.value = '';
        ui.typeFilter.value = 'ALL';
        ui.severityFilter.value = 'ALL';
        ui.statusFilter.value = 'ALL';
    }

    function resetForm() {
        state.editingRuleId = null;
        ui.ruleForm.reset();
        ui.ruleIdField.value = '';
        ui.ruleModalTitle.textContent = 'Add Rule';
        ui.saveRuleBtn.textContent = 'Save';
        ui.ruleSeverityField.value = 'MEDIUM';
        ui.ruleStatusField.value = 'ACTIVE';
        ui.ruleTypeField.value = 'AMOUNT_THRESHOLD';
        renderDynamicFields('AMOUNT_THRESHOLD');
    }

    function openEditForm(rule) {
        state.editingRuleId = rule.id;
        ui.ruleIdField.value = String(rule.id);
        ui.ruleNameField.value = rule.name || '';
        ui.ruleTypeField.value = rule.type || '';
        ui.ruleSeverityField.value = rule.severity || 'MEDIUM';
        ui.ruleStatusField.value = rule.active ? 'ACTIVE' : 'INACTIVE';
        ui.ruleModalTitle.textContent = 'Edit Rule';
        ui.saveRuleBtn.textContent = 'Update';
        renderDynamicFields(rule.type, rule);
        ruleModal.show();
    }

    function collectFormData() {
        const name = ui.ruleNameField.value.trim();
        const type = ui.ruleTypeField.value;
        const severity = ui.ruleSeverityField.value;
        const active = ui.ruleStatusField.value === 'ACTIVE';
        const config = readDynamicFields(type);

        if (!name || !type || !validateDynamicFields(type, config)) {
            return null;
        }

        return {
            name,
            type,
            severity,
            active,
            threshold: config.threshold,
            timeWindow: config.timeWindow,
            maxTransactions: config.maxTransactions
        };
    }

    async function fetchJson(url, options) {
        const response = await fetch(url, options);
        let payload = null;
        if (response.status !== 204) {
            payload = await response.json().catch(() => null);
        }
        if (!response.ok) {
            const message = payload && payload.message ? payload.message : `Request failed (${response.status})`;
            throw new Error(message);
        }
        return payload;
    }

    async function loadRules() {
        const rules = await fetchJson(`${API_BASE_URL}/rules`);
        state.rules = Array.isArray(rules) ? rules : [];
        renderAll();
    }

    async function createRule(rule) {
        await fetchJson(`${API_BASE_URL}/rules`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(rule)
        });
    }

    async function updateRule(id, rule) {
        await fetchJson(`${API_BASE_URL}/rules/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(rule)
        });
    }

    async function deleteRule(id) {
        await fetchJson(`${API_BASE_URL}/rules/${id}`, { method: 'DELETE' });
    }

    function notifyRulesChanged() {
        window.dispatchEvent(new CustomEvent('rules:changed'));
        window.localStorage.setItem('rulesUpdatedAt', String(Date.now()));
    }

    function getRuleById(id) {
        return state.rules.find(rule => rule.id === id) || null;
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

    ui.ruleForm.addEventListener('submit', async event => {
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

        try {
            if (state.editingRuleId == null) {
                await createRule(payload);
            } else {
                await updateRule(state.editingRuleId, payload);
            }
            ruleModal.hide();
            await loadRules();
            notifyRulesChanged();
        } catch (error) {
            window.alert(error.message || 'Failed to save rule.');
        }
    });

    ui.resetFiltersBtn.addEventListener('click', () => {
        resetAllFilters();
        renderTable();
    });

    ui.searchInput.addEventListener('input', renderTable);
    ui.typeFilter.addEventListener('change', renderTable);
    ui.severityFilter.addEventListener('change', renderTable);
    ui.statusFilter.addEventListener('change', renderTable);

    rulesTableBody.addEventListener('click', async event => {
        const actionButton = event.target.closest('button[data-action]');
        if (!actionButton) {
            return;
        }

        const id = Number(actionButton.getAttribute('data-id'));
        const action = actionButton.getAttribute('data-action');
        const rule = getRuleById(id);
        if (!rule) {
            return;
        }

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
            if (!approved) {
                return;
            }
            try {
                await deleteRule(rule.id);
                await loadRules();
                notifyRulesChanged();
            } catch (error) {
                window.alert(error.message || 'Failed to delete rule.');
            }
            return;
        }

        if (action === 'toggle') {
            try {
                await updateRule(rule.id, {
                    name: rule.name,
                    type: rule.type,
                    severity: rule.severity,
                    threshold: rule.threshold,
                    timeWindow: rule.timeWindow,
                    maxTransactions: rule.maxTransactions,
                    active: !rule.active
                });
                await loadRules();
                notifyRulesChanged();
            } catch (error) {
                window.alert(error.message || 'Failed to change rule status.');
            }
        }
    });

    ui.ruleModalEl.addEventListener('hidden.bs.modal', resetForm);

    renderTypeSelects();
    resetForm();
    loadRules().catch(() => {
        state.rules = [];
        renderAll();
        window.alert('Could not load rules from backend.');
    });
});
