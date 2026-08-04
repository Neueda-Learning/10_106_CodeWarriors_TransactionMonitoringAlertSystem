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

    console.log("Frontend Skeleton initialized successfully.");
});
