document.addEventListener('DOMContentLoaded', () => {
    const API_BASE_URL = window.localStorage.getItem('apiBaseUrl') || 'http://10.9.65.182:8085';

    let transactions = [];
    let alerts = [];

    const navLinks = document.querySelectorAll('.nav-link');
    const viewSections = document.querySelectorAll('.view-section');

    navLinks.forEach(link => {
        link.addEventListener('click', event => {
            event.preventDefault();
            navLinks.forEach(nav => nav.classList.remove('active'));
            link.classList.add('active');

            viewSections.forEach(section => {
                section.classList.add('d-none');
                section.classList.remove('active');
            });

            const targetId = link.getAttribute('data-target');
            if (!targetId) {
                return;
            }
            const target = document.getElementById(targetId);
            if (target) {
                target.classList.remove('d-none');
                target.classList.add('active');
            }
        });
    });

    const txTableBody = document.getElementById('transactions-table-body');
    const txSearchInput = document.getElementById('tx-search');
    const txStageFilter = document.getElementById('tx-stage-filter');
    const txAmountFilter = document.getElementById('tx-amount-filter');
    const alertsTableBody = document.getElementById('alerts-table-body');
    const alertSearchInput = document.getElementById('alert-search');
    const alertSeverityFilter = document.getElementById('alert-severity-filter');
    const alertStatusFilter = document.getElementById('alert-status-filter');

    let txStageChartInstance = null;
    let txVolumeChartInstance = null;
    let alertSeverityChartInstance = null;
    let alertStatusChartInstance = null;
    let txAmountBucketChartInstance = null;

    function toNumber(value) {
        const parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : 0;
    }

    function formatMoney(value) {
        return `$${toNumber(value).toFixed(2)}`;
    }

    function formatDateTime(value) {
        if (!value) {
            return '-';
        }
        if (Array.isArray(value)) {
            const year = value[0];
            const month = String(value[1]).padStart(2, '0');
            const day = String(value[2]).padStart(2, '0');
            const hour = String(value[3] || 0).padStart(2, '0');
            const minute = String(value[4] || 0).padStart(2, '0');
            return `${year}-${month}-${day} ${hour}:${minute}`;
        }
        return String(value).replace('T', ' ').replace('Z', '');
    }

    function formatDateOnly(value) {
        const dateTime = formatDateTime(value);
        return dateTime === '-' ? '-' : dateTime.substring(0, 10);
    }

    function isAlertActive(alert) {
        return alert.newStatus !== 'CLOSED' && alert.newStatus !== 'DISMISSED';
    }

    function severityColor(severity) {
        if (severity === 'HIGH') {
            return 'danger';
        }
        if (severity === 'MEDIUM') {
            return 'warning';
        }
        if (severity === 'LOW') {
            return 'info';
        }
        return 'secondary';
    }

    function statusColor(status) {
        if (status === 'CLOSED' || status === 'DISMISSED') {
            return 'secondary';
        }
        if (status === 'OPEN') {
            return 'danger';
        }
        if (status === 'ACKNOWLEDGED') {
            return 'warning';
        }
        if (status === 'INVESTIGATING') {
            return 'info';
        }
        if (status === 'COMPLETED') {
            return 'success';
        }
        if (status === 'FAILED') {
            return 'danger';
        }
        if (status === 'PENDING') {
            return 'warning';
        }
        return 'secondary';
    }

    async function fetchJson(url, options) {
        const response = await fetch(url, options);
        if (!response.ok) {
            throw new Error(`Request failed (${response.status})`);
        }
        if (response.status === 204) {
            return null;
        }
        return response.json();
    }

    function renderCharts() {
        const statusCounts = { COMPLETED: 0, PENDING: 0, FAILED: 0 };
        transactions.forEach(tx => {
            if (statusCounts[tx.status] !== undefined) {
                statusCounts[tx.status] += 1;
            }
        });

        const stageCtx = document.getElementById('txStageChart');
        if (stageCtx) {
            if (txStageChartInstance) {
                txStageChartInstance.destroy();
            }
            txStageChartInstance = new Chart(stageCtx, {
                type: 'doughnut',
                data: {
                    labels: ['Completed', 'Pending', 'Failed'],
                    datasets: [{
                        data: [statusCounts.COMPLETED, statusCounts.PENDING, statusCounts.FAILED],
                        backgroundColor: ['#198754', '#ffc107', '#dc3545']
                    }]
                },
                options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'right' } } }
            });
        }

        const volumeByDate = {};
        transactions.forEach(tx => {
            const dateKey = formatDateOnly(tx.transactionTime);
            if (!volumeByDate[dateKey]) {
                volumeByDate[dateKey] = 0;
            }
            volumeByDate[dateKey] += toNumber(tx.amount);
        });

        const sortedDates = Object.keys(volumeByDate).sort().slice(-7);
        const labels = sortedDates.length > 0 ? sortedDates : ['No Data'];
        const values = sortedDates.length > 0 ? sortedDates.map(key => volumeByDate[key]) : [0];

        const volumeCtx = document.getElementById('txVolumeChart');
        if (volumeCtx) {
            if (txVolumeChartInstance) {
                txVolumeChartInstance.destroy();
            }
            txVolumeChartInstance = new Chart(volumeCtx, {
                type: 'bar',
                data: {
                    labels,
                    datasets: [{
                        label: 'Volume ($)',
                        data: values,
                        backgroundColor: 'rgba(13, 110, 253, 0.5)',
                        borderColor: '#0d6efd',
                        borderWidth: 1
                    }]
                },
                options: { responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true } } }
            });
        }

        const severityCounts = { HIGH: 0, MEDIUM: 0, LOW: 0 };
        alerts.forEach(alert => {
            if (severityCounts[alert.severity] !== undefined) {
                severityCounts[alert.severity] += 1;
            }
        });

        const severityCtx = document.getElementById('alertSeverityChart');
        if (severityCtx) {
            if (alertSeverityChartInstance) {
                alertSeverityChartInstance.destroy();
            }
            alertSeverityChartInstance = new Chart(severityCtx, {
                type: 'pie',
                data: {
                    labels: ['High', 'Medium', 'Low'],
                    datasets: [{
                        data: [severityCounts.HIGH, severityCounts.MEDIUM, severityCounts.LOW],
                        backgroundColor: ['#dc3545', '#ffc107', '#0dcaf0']
                    }]
                },
                options: { responsive: true, maintainAspectRatio: false }
            });
        }

        const alertStageCounts = { OPEN: 0, ACKNOWLEDGED: 0, INVESTIGATING: 0, CLOSED: 0, DISMISSED: 0 };
        alerts.forEach(alert => {
            if (alertStageCounts[alert.newStatus] !== undefined) {
                alertStageCounts[alert.newStatus] += 1;
            }
        });

        const alertStatusCtx = document.getElementById('alertStatusChart');
        if (alertStatusCtx) {
            if (alertStatusChartInstance) {
                alertStatusChartInstance.destroy();
            }
            alertStatusChartInstance = new Chart(alertStatusCtx, {
                type: 'bar',
                data: {
                    labels: ['Open', 'Acknowledged', 'Investigating', 'Closed', 'Dismissed'],
                    datasets: [{
                        label: 'Alerts',
                        data: [
                            alertStageCounts.OPEN,
                            alertStageCounts.ACKNOWLEDGED,
                            alertStageCounts.INVESTIGATING,
                            alertStageCounts.CLOSED,
                            alertStageCounts.DISMISSED
                        ],
                        backgroundColor: ['#dc3545', '#ffc107', '#0dcaf0', '#6c757d', '#adb5bd'],
                        borderRadius: 8,
                        barThickness: 18
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    indexAxis: 'y',
                    plugins: {
                        legend: { display: false },
                        tooltip: { enabled: true }
                    },
                    scales: {
                        x: {
                            beginAtZero: true,
                            ticks: { precision: 0 }
                        },
                        y: {
                            grid: { display: false }
                        }
                    }
                }
            });
        }

        const amountBucketCounts = { UNDER_500: 0, BETWEEN_500_2000: 0, OVER_2000: 0 };
        transactions.forEach(tx => {
            const amount = toNumber(tx.amount);
            if (amount < 500) {
                amountBucketCounts.UNDER_500 += 1;
            } else if (amount <= 2000) {
                amountBucketCounts.BETWEEN_500_2000 += 1;
            } else {
                amountBucketCounts.OVER_2000 += 1;
            }
        });

        const txBucketCtx = document.getElementById('txAmountBucketChart');
        if (txBucketCtx) {
            if (txAmountBucketChartInstance) {
                txAmountBucketChartInstance.destroy();
            }
            txAmountBucketChartInstance = new Chart(txBucketCtx, {
                type: 'doughnut',
                data: {
                    labels: ['< $500', '$500 - $2,000', '> $2,000'],
                    datasets: [{
                        data: [
                            amountBucketCounts.UNDER_500,
                            amountBucketCounts.BETWEEN_500_2000,
                            amountBucketCounts.OVER_2000
                        ],
                        backgroundColor: ['#20c997', '#0d6efd', '#6610f2']
                    }]
                },
                options: { responsive: true, maintainAspectRatio: false }
            });
        }
    }

    function updateDashboard() {
        const totalTx = transactions.length;
        const activeAlerts = alerts.filter(isAlertActive).length;
        const highSeverity = alerts.filter(alert => isAlertActive(alert) && alert.severity === 'HIGH').length;

        document.getElementById('summary-total-tx').textContent = totalTx;
        document.getElementById('summary-active-alerts').textContent = activeAlerts;
        document.getElementById('summary-high-severity').textContent = highSeverity;
        document.getElementById('nav-alert-badge').textContent = activeAlerts;

        const activityStream = document.getElementById('dashboard-activity-stream');
        activityStream.innerHTML = '';

        transactions.slice(0, 5).forEach(tx => {
            const li = document.createElement('li');
            li.className = 'list-group-item d-flex justify-content-between align-items-center py-3 px-4';
            li.innerHTML = `
                <div>
                    <span class="fw-bold">Transaction #${tx.id}</span>
                    <span class="text-muted ms-2 d-block d-sm-inline">from ACC-${tx.fromAccountId} to ACC-${tx.toAccountId}</span>
                    <div class="small text-muted mt-1"><i class="bi bi-clock"></i> ${formatDateTime(tx.transactionTime)}</div>
                </div>
                <div class="text-end">
                    <span class="badge bg-${statusColor(tx.status)} mb-1 d-block">${tx.status}</span>
                    <span class="fw-bold fs-5">${formatMoney(tx.amount)}</span>
                </div>
            `;
            activityStream.appendChild(li);
        });

        renderCharts();
    }

    function renderTransactions() {
        if (!txTableBody) {
            return;
        }
        const searchTerm = txSearchInput ? txSearchInput.value.trim().toLowerCase() : '';
        const stageFilter = txStageFilter ? txStageFilter.value : 'ALL';
        const amountFilter = txAmountFilter ? txAmountFilter.value : 'ALL';

        const filtered = transactions.filter(tx => {
            const txId = String(tx.id || '');
            const from = String(tx.fromAccountId || '');
            const to = String(tx.toAccountId || '');
            const amount = toNumber(tx.amount);

            const matchesSearch = txId.includes(searchTerm) || from.includes(searchTerm) || to.includes(searchTerm);
            const matchesStatus = stageFilter === 'ALL' || tx.status === stageFilter;

            let matchesAmount = true;
            if (amountFilter === '0-500') {
                matchesAmount = amount < 500;
            } else if (amountFilter === '500-2000') {
                matchesAmount = amount >= 500 && amount <= 2000;
            } else if (amountFilter === '2000+') {
                matchesAmount = amount > 2000;
            }

            return matchesSearch && matchesStatus && matchesAmount;
        });

        txTableBody.innerHTML = '';
        if (filtered.length === 0) {
            txTableBody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">No transactions found matching your criteria.</td></tr>';
            return;
        }

        filtered.forEach(tx => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="fw-bold">#${tx.id}</td>
                <td>ACC-${tx.fromAccountId}</td>
                <td>ACC-${tx.toAccountId}</td>
                <td class="fw-bold">${formatMoney(tx.amount)}</td>
                <td><small class="text-muted">${formatDateOnly(tx.transactionTime)}</small></td>
                <td><span class="badge bg-${statusColor(tx.status)}">${tx.status}</span></td>
            `;
            txTableBody.appendChild(tr);
        });
    }

    function renderAlerts() {
        if (!alertsTableBody || !alertSearchInput || !alertStatusFilter || !alertSeverityFilter) {
            return;
        }

        const searchTerm = alertSearchInput.value.trim().toLowerCase();
        const severityFilter = alertSeverityFilter.value;
        const statusFilter = alertStatusFilter.value;

        const filtered = alerts.filter(alert => {
            const matchesSearch = String(alert.id).includes(searchTerm) || String(alert.transactionId).includes(searchTerm);
            const matchesSeverity = severityFilter === 'ALL' || alert.severity === severityFilter;
            const matchesStatus = statusFilter === 'ALL' || alert.newStatus === statusFilter;
            return matchesSearch && matchesSeverity && matchesStatus;
        });

        alertsTableBody.innerHTML = '';
        if (filtered.length === 0) {
            alertsTableBody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">No alerts found matching your criteria.</td></tr>';
            return;
        }

        filtered.forEach(alert => {
            const tr = document.createElement('tr');
            const currentStatus = alert.newStatus || 'OPEN';
            const canAcknowledge = currentStatus === 'OPEN';
            const canInvestigate = currentStatus === 'ACKNOWLEDGED';
            const canResolve = currentStatus === 'INVESTIGATING';
            tr.innerHTML = `
                <td class="fw-bold">ALT-${alert.id}</td>
                <td>#${alert.transactionId}</td>
                <td>${alert.alertReason || '-'}</td>
                <td><span class="badge bg-${severityColor(alert.severity)}">${alert.severity || 'N/A'}</span></td>
                <td><span class="badge bg-${statusColor(currentStatus)}">${currentStatus}</span></td>
                <td>
                    <button class="btn btn-sm btn-outline-dark me-1" data-action="view" data-id="${alert.id}" title="View Details"><i class="bi bi-eye"></i></button>
                    <button class="btn btn-sm btn-outline-primary me-1" data-action="ack" data-id="${alert.id}" ${canAcknowledge ? '' : 'disabled'} title="Acknowledge"><i class="bi bi-check-circle"></i></button>
                    <button class="btn btn-sm btn-outline-info me-1" data-action="investigate" data-id="${alert.id}" ${canInvestigate ? '' : 'disabled'} title="Investigate"><i class="bi bi-search"></i></button>
                    <button class="btn btn-sm btn-outline-success me-1" data-action="close" data-id="${alert.id}" ${canResolve ? '' : 'disabled'} title="Close"><i class="bi bi-shield-check"></i></button>
                    <button class="btn btn-sm btn-outline-secondary" data-action="dismiss" data-id="${alert.id}" ${canResolve ? '' : 'disabled'} title="Dismiss"><i class="bi bi-x-circle"></i></button>
                </td>
            `;
            alertsTableBody.appendChild(tr);
        });
    }

    async function loadDashboardData() {
        const transactionsPromise = fetchJson(`${API_BASE_URL}/transactions`);
        const alertsPromise = fetchJson(`${API_BASE_URL}/alerts`);

        const [transactionsResult, alertsResult] = await Promise.allSettled([transactionsPromise, alertsPromise]);

        transactions = transactionsResult.status === 'fulfilled' && Array.isArray(transactionsResult.value)
            ? transactionsResult.value
            : [];
        alerts = alertsResult.status === 'fulfilled' && Array.isArray(alertsResult.value)
            ? alertsResult.value
            : [];

        updateDashboard();
        renderTransactions();
        renderAlerts();
    }

    async function updateAlertStatus(alertId, targetStatus) {
        const alert = alerts.find(item => item.id === alertId);
        if (!alert) {
            return;
        }

        const payload = {
            oldStatus: alert.newStatus || 'OPEN',
            newStatus: targetStatus
        };

        await fetchJson(`${API_BASE_URL}/alerts/${alertId}/status`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        await loadDashboardData();
    }
    
    function viewAlertDetails(alertId) {
        const alert = alerts.find(item => item.id === alertId);
        if (!alert) return;
        
        const tx = transactions.find(item => item.id === alert.transactionId);
        
        const content = document.getElementById('alert-view-content');
        if (!content) return;
        
        const statusBadge = `<span class="badge bg-${statusColor(alert.newStatus || 'OPEN')}">${alert.newStatus || 'OPEN'}</span>`;
        const severityBadge = `<span class="badge bg-${severityColor(alert.severity)}">${alert.severity || 'N/A'}</span>`;
        
        let txHtml = '<p class="text-muted">Transaction details not available.</p>';
        if (tx) {
            txHtml = `
                <table class="table table-bordered mb-0">
                    <tbody>
                        <tr><th class="bg-light" style="width: 30%">Transaction ID</th><td>#${tx.id}</td></tr>
                        <tr><th class="bg-light">Amount</th><td class="fw-bold">${formatMoney(tx.amount)} ${tx.currency || 'USD'}</td></tr>
                        <tr><th class="bg-light">From Account</th><td>ACC-${tx.fromAccountId}</td></tr>
                        <tr><th class="bg-light">To Account</th><td>ACC-${tx.toAccountId}</td></tr>
                        <tr><th class="bg-light">Timestamp</th><td>${formatDateTime(tx.transactionTime)}</td></tr>
                        <tr><th class="bg-light">Status</th><td><span class="badge bg-${statusColor(tx.status)}">${tx.status}</span></td></tr>
                    </tbody>
                </table>
            `;
        }
        
        content.innerHTML = `
            <div class="row g-4">
                <div class="col-md-12">
                    <h5 class="border-bottom pb-2 mb-3">Alert Information</h5>
                    <table class="table table-bordered mb-0">
                        <tbody>
                            <tr><th class="bg-light" style="width: 30%">Alert ID</th><td>ALT-${alert.id}</td></tr>
                            <tr><th class="bg-light">Rule ID</th><td>${alert.ruleId}</td></tr>
                            <tr><th class="bg-light">Severity</th><td>${severityBadge}</td></tr>
                            <tr><th class="bg-light">Status</th><td>${statusBadge}</td></tr>
                            <tr><th class="bg-light">Reason</th><td>${alert.alertReason || '-'}</td></tr>
                        </tbody>
                    </table>
                </div>
                <div class="col-md-12">
                    <h5 class="border-bottom pb-2 mb-3">Triggering Transaction</h5>
                    ${txHtml}
                </div>
            </div>
        `;
        
        const modalEl = document.getElementById('alertViewModal');
        const modal = new bootstrap.Modal(modalEl);
        modal.show();
    }

    if (txSearchInput) {
        txSearchInput.addEventListener('input', renderTransactions);
    }
    if (txStageFilter) {
        txStageFilter.addEventListener('change', renderTransactions);
    }
    if (txAmountFilter) {
        txAmountFilter.addEventListener('change', renderTransactions);
    }

    if (alertSearchInput) {
        alertSearchInput.addEventListener('input', renderAlerts);
    }
    if (alertSeverityFilter) {
        alertSeverityFilter.addEventListener('change', renderAlerts);
    }
    if (alertStatusFilter) {
        alertStatusFilter.addEventListener('change', renderAlerts);
    }
    if (alertsTableBody) {
        alertsTableBody.addEventListener('click', async event => {
            const button = event.target.closest('button[data-action]');
            if (!button) {
                return;
            }

            const id = Number(button.getAttribute('data-id'));
            const action = button.getAttribute('data-action');

            if (action === 'view') {
                viewAlertDetails(id);
                return;
            }

            try {
                if (action === 'ack') {
                    await updateAlertStatus(id, 'ACKNOWLEDGED');
                }
                if (action === 'investigate') {
                    await updateAlertStatus(id, 'INVESTIGATING');
                }
                if (action === 'close') {
                    await updateAlertStatus(id, 'CLOSED');
                }
                if (action === 'dismiss') {
                    await updateAlertStatus(id, 'DISMISSED');
                }
            } catch (error) {
                window.alert('Could not update alert status. Check backend logs and try again.');
            }
        });
    }

    loadDashboardData().catch(() => {
        updateDashboard();
        renderTransactions();
        renderAlerts();
    });

    // Expose refresh for the rules module and listen for rule-change signals.
    window.loadDashboardData = loadDashboardData;
    window.addEventListener('rules:changed', () => {
        loadDashboardData().catch(() => {
            // Keep current data in UI when refresh fails.
        });
    });
    window.addEventListener('storage', event => {
        if (event.key === 'rulesUpdatedAt') {
            loadDashboardData().catch(() => {
                // Keep current data in UI when refresh fails.
            });
        }
    });

    setInterval(() => {
        loadDashboardData().catch(() => {
            // Keep current data in UI when refresh fails.
        });
    }, 60000);
});


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

