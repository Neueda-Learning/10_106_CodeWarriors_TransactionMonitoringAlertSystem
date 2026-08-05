document.addEventListener('DOMContentLoaded', () => {
    const API_BASE_URL = window.localStorage.getItem('apiBaseUrl') || 'http://localhost:8085';

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
