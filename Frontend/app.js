document.addEventListener('DOMContentLoaded', () => {
    
    // Simple SPA Navigation Logic
    const navLinks = document.querySelectorAll('.nav-link');
    const viewSections = document.querySelectorAll('.view-section');

    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            
            // Remove active class from all nav links
            navLinks.forEach(nav => nav.classList.remove('active'));
            // Add active class to clicked link
            e.target.classList.add('active');

            // Hide all sections
            viewSections.forEach(section => {
                section.classList.add('d-none');
                section.classList.remove('active');
            });

            // Show target section
            const targetId = e.target.getAttribute('data-target');
            if (targetId) {
                const targetElement = document.getElementById(targetId);
                if (targetElement) {
                    targetElement.classList.remove('d-none');
                    targetElement.classList.add('active');
                }
            }
        });
    });

    // --- Dashboard View Logic ---
    let txStageChartInstance = null;
    let txVolumeChartInstance = null;

    function updateDashboard() {
        const totalTx = transactions ? transactions.length : 0;
        const activeAlerts = alerts.filter(a => a.newStatus !== 'CLOSED' && a.newStatus !== 'DISMISSED').length;
        const highSeverity = alerts.filter(a => a.severity === 'HIGH' && a.newStatus !== 'CLOSED' && a.newStatus !== 'DISMISSED').length;
        
        document.getElementById('summary-total-tx').textContent = totalTx;
        document.getElementById('summary-active-alerts').textContent = activeAlerts;
        document.getElementById('summary-high-severity').textContent = highSeverity;
        document.getElementById('nav-alert-badge').textContent = activeAlerts;
        
        const activityStream = document.getElementById('dashboard-activity-stream');
        activityStream.innerHTML = '';
        
        if (!transactions || transactions.length === 0) return;
        
        // Take latest 5 transactions for activity stream (backend returns descending)
        const recentTx = transactions.slice(0, 5);
        recentTx.forEach(tx => {
            const li = document.createElement('li');
            li.className = 'list-group-item d-flex justify-content-between align-items-center py-3 px-4';
            const statusColor = tx.status === 'COMPLETED' ? 'success' : (tx.status === 'FAILED' ? 'danger' : 'warning');
            
            li.innerHTML = `
                <div>
                    <span class="fw-bold">Transaction #${tx.id}</span>
                    <span class="text-muted ms-2 d-block d-sm-inline">from ACC-${tx.fromAccountId} to ACC-${tx.toAccountId}</span>
                    <div class="small text-muted mt-1"><i class="bi bi-clock"></i> ${Array.isArray(tx.transactionTime) ? tx.transactionTime[0]+'-'+String(tx.transactionTime[1]).padStart(2,'0')+'-'+String(tx.transactionTime[2]).padStart(2,'0') + ' ' + String(tx.transactionTime[3]).padStart(2,'0')+':'+String(tx.transactionTime[4]).padStart(2,'0') : String(tx.transactionTime).replace('T', ' ')}</div>
                </div>
                <div class="text-end">
                    <span class="badge bg-${statusColor} mb-1 d-block">${tx.status}</span>
                    <span class="fw-bold fs-5">$${tx.amount.toFixed(2)}</span>
                </div>
            `;
            activityStream.appendChild(li);
        });

        // Initialize Charts
        renderCharts();
    }

    function renderCharts() {
        // Prepare Data for Stage Chart
        const statusCounts = { COMPLETED: 0, PENDING: 0, FAILED: 0 };
        transactions.forEach(tx => {
            if (statusCounts[tx.status] !== undefined) statusCounts[tx.status]++;
        });

        const stageCtx = document.getElementById('txStageChart');
        if (stageCtx) {
            if (txStageChartInstance) txStageChartInstance.destroy();
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

        // Prepare Dynamic Data for Volume Chart
        const volumeByDate = {};
        transactions.forEach(tx => {
            let dateStr = "Unknown";
            if (Array.isArray(tx.transactionTime)) {
                dateStr = tx.transactionTime[0] + "-" + String(tx.transactionTime[1]).padStart(2, '0') + "-" + String(tx.transactionTime[2]).padStart(2, '0');
            } else if (tx.transactionTime) {
                dateStr = String(tx.transactionTime).substring(0, 10);
            }
            if (!volumeByDate[dateStr]) volumeByDate[dateStr] = 0;
            volumeByDate[dateStr] += tx.amount;
        });
        
        const sortedDates = Object.keys(volumeByDate).sort().slice(-7); // Last 7 days
        const chartLabels = sortedDates.length > 0 ? sortedDates : ['No Data'];
        const chartData = sortedDates.length > 0 ? sortedDates.map(d => volumeByDate[d]) : [0];

        const volumeCtx = document.getElementById('txVolumeChart');
        if (volumeCtx) {
            if (txVolumeChartInstance) txVolumeChartInstance.destroy();
            txVolumeChartInstance = new Chart(volumeCtx, {
                type: 'bar',
                data: {
                    labels: chartLabels,
                    datasets: [{
                        label: 'Volume ($)',
                        data: chartData,
                        backgroundColor: 'rgba(13, 110, 253, 0.5)',
                        borderColor: '#0d6efd',
                        borderWidth: 1
                    }]
                },
                options: { responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true } } }
            });
        }
    }

    // --- Transactions List Logic ---
    const txTableBody = document.getElementById('transactions-table-body');
    const txSearchInput = document.getElementById('tx-search');
    const txStageFilter = document.getElementById('tx-stage-filter');
    const txAmountFilter = document.getElementById('tx-amount-filter');

    function renderTransactions() {
        if (!txTableBody) return;
        
        const searchTerm = txSearchInput ? txSearchInput.value.toLowerCase() : '';
        const stageFilter = txStageFilter ? txStageFilter.value : 'ALL';
        const amountFilter = txAmountFilter ? txAmountFilter.value : 'ALL';
        
        txTableBody.innerHTML = '';
        
        const filteredTx = transactions.filter(tx => {
            const matchesSearch = tx.id.toString().includes(searchTerm) || 
                                  tx.fromAccountId.toString().includes(searchTerm) || 
                                  tx.toAccountId.toString().includes(searchTerm);
                                  
            const matchesStage = stageFilter === 'ALL' || tx.status === stageFilter;
            
            let matchesAmount = true;
            if (amountFilter === '0-500') matchesAmount = tx.amount < 500;
            else if (amountFilter === '500-2000') matchesAmount = tx.amount >= 500 && tx.amount <= 2000;
            else if (amountFilter === '2000+') matchesAmount = tx.amount > 2000;
            
            return matchesSearch && matchesStage && matchesAmount;
        });
        
        if (filteredTx.length === 0) {
            txTableBody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">No transactions found matching your criteria.</td></tr>';
            return;
        }

        filteredTx.forEach(tx => {
            const tr = document.createElement('tr');
            const statusColor = tx.status === 'COMPLETED' ? 'success' : (tx.status === 'FAILED' ? 'danger' : 'warning');
            
            tr.innerHTML = `
                <td class="fw-bold">#${tx.id}</td>
                <td>ACC-${tx.fromAccountId}</td>
                <td>ACC-${tx.toAccountId}</td>
                <td class="fw-bold">$${tx.amount.toFixed(2)}</td>
                <td><small class="text-muted">${Array.isArray(tx.transactionTime) ? tx.transactionTime[0]+'-'+String(tx.transactionTime[1]).padStart(2,'0')+'-'+String(tx.transactionTime[2]).padStart(2,'0') : String(tx.transactionTime).substring(0,10)}</small></td>
                <td><span class="badge bg-${statusColor}">${tx.status}</span></td>
            `;
            txTableBody.appendChild(tr);
        });
    }

    if (txSearchInput) txSearchInput.addEventListener('input', renderTransactions);
    if (txStageFilter) txStageFilter.addEventListener('change', renderTransactions);
    if (txAmountFilter) txAmountFilter.addEventListener('change', renderTransactions);

    // --- Database API Connection Logic ---
    async function initializeData() {
        try {
            // Attempt to fetch live data from the Database via Spring Boot API
            const response = await fetch('http://localhost:8085/api/transactions');
            if (response.ok) {
                transactions = await response.json();
                console.log("Successfully loaded transactions from Database!");
            } else {
                console.warn("Backend API returned an error, falling back to mock data.");
            }
        } catch (error) {
            console.warn("Backend API not reachable (Is Spring Boot running?), falling back to mock data.");
        } finally {
            // Render UI with either Live DB data or Mock fallback data
            updateDashboard();
            if (txTableBody) renderTransactions();
        }
    }

    // Start initialization
    initializeData();
    // Auto-refresh every 60 seconds for live dashboard
    setInterval(initializeData, 60000);

    // --- Alerts List Logic ---
    let alerts = [
        { id: 101, transactionId: 2, ruleId: 1, alertReason: 'High Velocity Check', severity: 'MEDIUM', oldStatus: 'NONE', newStatus: 'OPEN' },
        { id: 102, transactionId: 5, ruleId: 2, alertReason: 'Large Transaction', severity: 'HIGH', oldStatus: 'NONE', newStatus: 'ACKNOWLEDGED' },
        { id: 103, transactionId: 9, ruleId: 1, alertReason: 'High Velocity Check', severity: 'MEDIUM', oldStatus: 'NONE', newStatus: 'OPEN' },
        { id: 104, transactionId: 12, ruleId: 3, alertReason: 'Multiple Countries in 24h', severity: 'HIGH', oldStatus: 'OPEN', newStatus: 'INVESTIGATING' },
        { id: 105, transactionId: 18, ruleId: 4, alertReason: 'Unusual Time Check', severity: 'LOW', oldStatus: 'NONE', newStatus: 'OPEN' },
        { id: 106, transactionId: 21, ruleId: 2, alertReason: 'Large Transaction', severity: 'HIGH', oldStatus: 'NONE', newStatus: 'OPEN' },
        { id: 107, transactionId: 33, ruleId: 1, alertReason: 'High Velocity Check', severity: 'MEDIUM', oldStatus: 'OPEN', newStatus: 'CLOSED' },
        { id: 108, transactionId: 45, ruleId: 5, alertReason: 'Structuring Detection', severity: 'HIGH', oldStatus: 'NONE', newStatus: 'OPEN' },
        { id: 109, transactionId: 67, ruleId: 1, alertReason: 'High Velocity Check', severity: 'MEDIUM', oldStatus: 'NONE', newStatus: 'ACKNOWLEDGED' },
        { id: 110, transactionId: 89, ruleId: 2, alertReason: 'Large Transaction', severity: 'HIGH', oldStatus: 'INVESTIGATING', newStatus: 'CLOSED' },
        { id: 111, transactionId: 92, ruleId: 4, alertReason: 'Unusual Time Check', severity: 'LOW', oldStatus: 'NONE', newStatus: 'OPEN' },
        { id: 112, transactionId: 112, ruleId: 3, alertReason: 'Multiple Countries in 24h', severity: 'HIGH', oldStatus: 'NONE', newStatus: 'OPEN' },
        { id: 113, transactionId: 145, ruleId: 5, alertReason: 'Structuring Detection', severity: 'HIGH', oldStatus: 'NONE', newStatus: 'INVESTIGATING' },
        { id: 114, transactionId: 178, ruleId: 1, alertReason: 'High Velocity Check', severity: 'MEDIUM', oldStatus: 'NONE', newStatus: 'OPEN' },
        { id: 115, transactionId: 199, ruleId: 2, alertReason: 'Large Transaction', severity: 'HIGH', oldStatus: 'NONE', newStatus: 'OPEN' }
    ];

    const alertsTableBody = document.getElementById('alerts-table-body');
    const alertSearchInput = document.getElementById('alert-search');
    const alertStatusFilter = document.getElementById('alert-status-filter');

    function renderAlerts() {
        if (!alertsTableBody) return;
        
        const searchTerm = alertSearchInput.value.toLowerCase();
        const statusFilter = alertStatusFilter.value;
        
        alertsTableBody.innerHTML = '';
        
        const filteredAlerts = alerts.filter(al => {
            const matchesSearch = al.id.toString().includes(searchTerm) || 
                                  al.transactionId.toString().includes(searchTerm);
            const matchesStatus = statusFilter === 'ALL' || al.newStatus === statusFilter;
            
            return matchesSearch && matchesStatus;
        });
        
        if (filteredAlerts.length === 0) {
            alertsTableBody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">No alerts found matching your criteria.</td></tr>';
            return;
        }

        filteredAlerts.forEach(al => {
            const tr = document.createElement('tr');
            
            let severityColor = 'secondary';
            if (al.severity === 'HIGH') severityColor = 'danger';
            else if (al.severity === 'MEDIUM') severityColor = 'warning';
            else if (al.severity === 'LOW') severityColor = 'info';

            let statusColor = 'primary';
            if (al.newStatus === 'CLOSED' || al.newStatus === 'DISMISSED') statusColor = 'secondary';
            else if (al.newStatus === 'OPEN') statusColor = 'danger';
            else if (al.newStatus === 'ACKNOWLEDGED') statusColor = 'warning';
            else if (al.newStatus === 'INVESTIGATING') statusColor = 'info';
            
            tr.innerHTML = `
                <td class="fw-bold">ALT-${al.id}</td>
                <td><a href="#" class="text-decoration-none">#${al.transactionId}</a></td>
                <td>${al.alertReason}</td>
                <td><span class="badge bg-${severityColor}">${al.severity}</span></td>
                <td><span class="badge bg-${statusColor}">${al.newStatus}</span></td>
                <td>
                    <button class="btn btn-sm btn-outline-primary me-1" title="Acknowledge"><i class="bi bi-check-circle"></i></button>
                    <button class="btn btn-sm btn-outline-secondary" title="Dismiss"><i class="bi bi-x-circle"></i></button>
                </td>
            `;
            alertsTableBody.appendChild(tr);
        });
    }

    if (alertSearchInput && alertStatusFilter) {
        alertSearchInput.addEventListener('input', renderAlerts);
        alertStatusFilter.addEventListener('change', renderAlerts);
        renderAlerts(); // Initial render
    }

    console.log("Frontend Skeleton initialized successfully.");
});
