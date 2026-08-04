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
    function updateDashboard() {
        const totalTx = transactions.length;
        const activeAlerts = 3; // Mock data
        const highSeverity = 1; // Mock data
        
        document.getElementById('summary-total-tx').textContent = totalTx;
        document.getElementById('summary-active-alerts').textContent = activeAlerts;
        document.getElementById('summary-high-severity').textContent = highSeverity;
        document.getElementById('nav-alert-badge').textContent = activeAlerts;
        
        const activityStream = document.getElementById('dashboard-activity-stream');
        activityStream.innerHTML = '';
        
        // Take latest 3 transactions for activity stream
        const recentTx = transactions.slice(-3).reverse();
        recentTx.forEach(tx => {
            const li = document.createElement('li');
            li.className = 'list-group-item d-flex justify-content-between align-items-center py-3 px-4';
            
            const statusColor = tx.status === 'COMPLETED' ? 'success' : (tx.status === 'FAILED' ? 'danger' : 'warning');
            
            li.innerHTML = `
                <div>
                    <span class="fw-bold">Transaction #${tx.id}</span>
                    <span class="text-muted ms-2 d-block d-sm-inline">from ACC-${tx.fromAccountId} to ACC-${tx.toAccountId}</span>
                    <div class="small text-muted mt-1"><i class="bi bi-clock"></i> ${tx.transactionTime}</div>
                </div>
                <div class="text-end">
                    <span class="badge bg-${statusColor} mb-1 d-block">${tx.status}</span>
                    <span class="fw-bold fs-5">$${tx.amount.toFixed(2)}</span>
                </div>
            `;
            activityStream.appendChild(li);
        });
    }

    // Initialize Dashboard
    updateDashboard();

    // --- Transactions List Logic ---
    const txTableBody = document.getElementById('transactions-table-body');
    const txSearchInput = document.getElementById('tx-search');
    const txStatusFilter = document.getElementById('tx-status-filter');

    function renderTransactions() {
        if (!txTableBody) return;
        
        const searchTerm = txSearchInput.value.toLowerCase();
        const statusFilter = txStatusFilter.value;
        
        txTableBody.innerHTML = '';
        
        const filteredTx = transactions.filter(tx => {
            const matchesSearch = tx.id.toString().includes(searchTerm) || 
                                  tx.fromAccountId.toString().includes(searchTerm) || 
                                  tx.toAccountId.toString().includes(searchTerm);
                                  
            const matchesStatus = statusFilter === 'ALL' || tx.status === statusFilter;
            
            return matchesSearch && matchesStatus;
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
                <td><small class="text-muted">${tx.transactionTime}</small></td>
                <td><span class="badge bg-${statusColor}">${tx.status}</span></td>
            `;
            txTableBody.appendChild(tr);
        });
    }

    if (txSearchInput && txStatusFilter) {
        txSearchInput.addEventListener('input', renderTransactions);
        txStatusFilter.addEventListener('change', renderTransactions);
        renderTransactions(); // Initial render
    }

    // --- Alerts List Logic ---
    let alerts = [
        { id: 101, transactionId: 2, ruleId: 1, alertReason: 'High Velocity Check', severity: 'MEDIUM', oldStatus: 'NONE', newStatus: 'OPEN' },
        { id: 102, transactionId: 5, ruleId: 2, alertReason: 'Large Transaction', severity: 'HIGH', oldStatus: 'NONE', newStatus: 'ACKNOWLEDGED' },
        { id: 103, transactionId: 9, ruleId: 1, alertReason: 'High Velocity Check', severity: 'MEDIUM', oldStatus: 'NONE', newStatus: 'OPEN' }
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
