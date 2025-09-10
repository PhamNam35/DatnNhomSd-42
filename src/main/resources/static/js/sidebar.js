document.addEventListener("DOMContentLoaded", function () {
    const currentPath = window.location.pathname;

    // B1: Xử lý active cho link
    document.querySelectorAll(".nav-link:not([data-bs-toggle='collapse'])").forEach(link => {
        const linkPath = new URL(link.href, window.location.origin).pathname;
        if (currentPath === linkPath) {
            link.classList.add("active");

            // Tìm submenu cha (nếu có) và mở
            const submenu = link.closest(".collapse");
            if (submenu) {
                const parentToggle = document.querySelector(`[href="#${submenu.id}"]`);
                if (parentToggle) {
                    new bootstrap.Collapse(submenu, { toggle: false }).show();
                    parentToggle.classList.add("active-parent");

                    const icon = parentToggle.querySelector('.toggle-icon');
                    if (icon && !document.getElementById('sidebar').classList.contains('collapsed')) {
                        icon.classList.replace('fa-chevron-down', 'fa-chevron-up');
                    }
                }
            }
        }
    });

    // B2: Xử lý toggle icon khi click vào dropdown
    document.querySelectorAll('[data-bs-toggle="collapse"]').forEach(toggle => {
        const icon = toggle.querySelector('.toggle-icon');
        const targetId = toggle.getAttribute('href');
        const targetEl = document.querySelector(targetId);

        if (!targetEl) return;

        const collapse = new bootstrap.Collapse(targetEl, { toggle: false });

        toggle.addEventListener("click", () => {
            // Mũi tên sẽ xoay thông qua sự kiện bên dưới
        });

        targetEl.addEventListener('show.bs.collapse', () => {
            if (!document.getElementById('sidebar').classList.contains('collapsed')) {
                icon?.classList.replace('fa-chevron-down', 'fa-chevron-up');
            }
            toggle.classList.add('active-parent');
        });

        targetEl.addEventListener('hide.bs.collapse', () => {
            icon?.classList.replace('fa-chevron-up', 'fa-chevron-down');
            toggle.classList.remove('active-parent');
        });
    });
});

document.addEventListener("DOMContentLoaded", function () {
    const sidebar = document.getElementById('sidebar');
    const toggleBtn = document.getElementById('toggleBtn');
    const toggleIcon = document.getElementById('toggleIcon');
    const content = document.querySelector('.content');
    const navbar = document.querySelector('.navbar');

    // Áp dụng trạng thái từ localStorage ngay khi tải trang
    const isCollapsed = localStorage.getItem('sidebarCollapsed') === 'true';
    if (isCollapsed) {
        sidebar.classList.add('collapsed');
        content.classList.add('collapsed');
        navbar.classList.add('collapsed');
        toggleBtn.classList.add('collapsed');
        toggleIcon.classList.remove('fa-bars');
        toggleIcon.classList.add('fa-arrow-right');
        document.querySelectorAll('.toggle-icon').forEach(icon => {
            icon.style.display = 'none';
        });
    } else {
        sidebar.classList.remove('collapsed');
        content.classList.remove('collapsed');
        navbar.classList.remove('collapsed');
        toggleBtn.classList.remove('collapsed');
        toggleIcon.classList.remove('fa-arrow-right');
        toggleIcon.classList.add('fa-bars');
        document.querySelectorAll('.toggle-icon').forEach(icon => {
            icon.style.display = 'inline-block';
        });
    }

    // Xử lý sự kiện toggle
    toggleBtn.addEventListener('click', function () {
        sidebar.classList.toggle('collapsed');
        content.classList.toggle('collapsed');
        navbar.classList.toggle('collapsed');
        toggleBtn.classList.toggle('collapsed');

        const isNowCollapsed = sidebar.classList.contains('collapsed');
        localStorage.setItem('sidebarCollapsed', isNowCollapsed);

        if (isNowCollapsed) {
            toggleIcon.classList.remove('fa-bars');
            toggleIcon.classList.add('fa-arrow-right');
            document.querySelectorAll('.toggle-icon').forEach(icon => {
                icon.style.display = 'none';
            });
        } else {
            toggleIcon.classList.remove('fa-arrow-right');
            toggleIcon.classList.add('fa-bars');
            document.querySelectorAll('.toggle-icon').forEach(icon => {
                icon.style.display = 'inline-block';
            });
        }
    });

    // Xử lý responsive
    window.addEventListener('resize', function () {
        if (window.innerWidth <= 768) {
            sidebar.classList.add('collapsed');
            content.classList.add('collapsed');
            navbar.classList.add('collapsed');
            toggleBtn.classList.remove('collapsed');
            toggleBtn.style.left = '15px';
            localStorage.setItem('sidebarCollapsed', 'true');
            toggleIcon.classList.remove('fa-bars');
            toggleIcon.classList.add('fa-arrow-right');
            document.querySelectorAll('.toggle-icon').forEach(icon => {
                icon.style.display = 'none';
            });
        } else {
            const storedState = localStorage.getItem('sidebarCollapsed') === 'true';
            if (storedState) {
                sidebar.classList.add('collapsed');
                content.classList.add('collapsed');
                navbar.classList.add('collapsed');
                toggleBtn.classList.add('collapsed');
                toggleIcon.classList.remove('fa-bars');
                toggleIcon.classList.add('fa-arrow-right');
                document.querySelectorAll('.toggle-icon').forEach(icon => {
                    icon.style.display = 'none';
                });
            } else {
                sidebar.classList.remove('collapsed');
                content.classList.remove('collapsed');
                navbar.classList.remove('collapsed');
                toggleBtn.classList.remove('collapsed');
                toggleIcon.classList.remove('fa-arrow-right');
                toggleIcon.classList.add('fa-bars');
                document.querySelectorAll('.toggle-icon').forEach(icon => {
                    icon.style.display = 'inline-block';
                });
            }
            toggleBtn.style.left = '65px';
        }
    });

    // Khởi tạo responsive khi tải trang
    window.addEventListener('load', function () {
        if (window.innerWidth <= 768) {
            sidebar.classList.add('collapsed');
            content.classList.add('collapsed');
            navbar.classList.add('collapsed');
            toggleBtn.classList.remove('collapsed');
            toggleBtn.style.left = '15px';
            localStorage.setItem('sidebarCollapsed', 'true');
            toggleIcon.classList.remove('fa-bars');
            toggleIcon.classList.add('fa-arrow-right');
            document.querySelectorAll('.toggle-icon').forEach(icon => {
                icon.style.display = 'none';
            });
        }
    });
});