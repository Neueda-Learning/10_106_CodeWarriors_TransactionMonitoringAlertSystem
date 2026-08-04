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

    console.log("Frontend Skeleton initialized successfully.");
});
