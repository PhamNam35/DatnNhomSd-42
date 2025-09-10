document.addEventListener('DOMContentLoaded', function () {
    console.log('DOM loaded, initializing notification system...');

    if (typeof bootstrap === 'undefined') {
        console.error('Bootstrap JavaScript not loaded. Ensure Bootstrap 5.1.3 is included.');
        alert('Lỗi hệ thống: Không thể tải thư viện giao diện. Vui lòng thử lại sau.');
        return;
    }
    const markingSet = new Set(); // Để tránh xử lý cùng 1 id nhiều lần

    let currentPage = 0;
    let totalPages = 1;
    let unreadCount = 0;
    let isDropdownOpen = false;
    let cachedNotifications = null;
    let lastFetchTime = 0;
    const CACHE_DURATION = 30000; // Cache trong 30 giây

    // ====== AUTO REFRESH UNREAD BADGE ======
    const UNREAD_POLL_INTERVAL = 5000; // 5s – muốn nhanh/chậm chỉnh ở đây
    let unreadPollTimer = null;

    function pollUnreadCount() {
        fetch('/acvstore/thong-bao/load?unread=true&_=' + Date.now(), { cache: 'no-store' })
            .then(res => res.ok ? res.json() : Promise.reject(res.status))
            .then(data => {
                const serverCount = typeof data?.unreadCount === 'number' ? data.unreadCount : 0;

                if (serverCount !== unreadCount) {
                    updateUnreadCount(serverCount);

                    // Nếu dropdown đang mở thì refresh luôn danh sách
                    if (isDropdownOpen) {
                        cachedNotifications = null;   // xoá cache để lần này load mới
                        loadNotifications();
                    }
                }
            })
            .catch(() => { /* im lặng nếu lỗi mạng */ });
    }

    function startUnreadAutoRefresh() {
        if (unreadPollTimer) clearInterval(unreadPollTimer);
        pollUnreadCount(); // gọi ngay khi vào trang
        unreadPollTimer = setInterval(pollUnreadCount, UNREAD_POLL_INTERVAL);
    }

    // Dừng/khởi động lại khi tab ẩn/hiện để tiết kiệm tài nguyên
    document.addEventListener('visibilitychange', () => {
        if (document.hidden) {
            if (unreadPollTimer) clearInterval(unreadPollTimer);
        } else {
            startUnreadAutoRefresh();
        }
    });
    // ====== END AUTO REFRESH ======

    // Hàm debounce để ngăn gọi API nhiều lần
    function debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    // Hàm hiển thị toast
    function showToast(message, type = 'success') {
        const toastContainer = document.createElement('div');
        toastContainer.className = `position-fixed top-0 end-0 p-3`;
        toastContainer.style.zIndex = '1050';
        toastContainer.innerHTML = `
            <div class="toast align-items-center text-white bg-${type} border-0" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">${message}</div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
            </div>
        `;
        document.body.appendChild(toastContainer);
        const toast = new bootstrap.Toast(toastContainer.querySelector('.toast'));
        toast.show();
        setTimeout(() => toastContainer.remove(), 3000);
    }

    // Xử lý chuông thông báo
    const trigger = document.getElementById('notificationDropdown');
    if (trigger) {
        const dropdown = trigger.closest('.dropdown');
        if (dropdown) {
            trigger.addEventListener('click', debounce(function () {
                console.log('Notification bell clicked, triggering loadNotifications...');
                isDropdownOpen = true;
                loadNotifications();
            }, 300));
            const dropdownMenu = document.getElementById('notificationContainer');
            dropdownMenu.addEventListener('hidden.bs.dropdown', function () {
                isDropdownOpen = false;
            });
        } else {
            console.warn('Dropdown container not found for notificationDropdown');
        }
    } else {
        console.warn('Notification dropdown trigger not found');
    }

    // Tải thông báo cho bảng nếu có
    const tableBody = document.getElementById('notificationTableBody');
    if (tableBody) {
        loadNotificationsForTable();
    }

    // Xử lý liên kết "Xem tất cả"
    const viewAllLinks = document.querySelectorAll('a[data-bs-target="#notificationModal"]');
    viewAllLinks.forEach(link => {
        link.addEventListener('click', function (e) {
            e.preventDefault();
            console.log('View all notifications link clicked');
            const modal = document.getElementById('notificationModal');
            if (!modal) {
                console.error('Modal element not found for ID: notificationModal');
                alert('Lỗi hệ thống: Không tìm thấy cửa sổ thông báo.');
                return;
            }
            try {
                console.log('Attempting to initialize and show modal');
                const modalInstance = new bootstrap.Modal(modal);
                modalInstance.show();
                console.log('Modal opened successfully');
                modal.addEventListener('shown.bs.modal', function () {
                    console.log('Modal is shown, loading notifications...');
                    currentPage = 0;
                    loadNotificationsForModal();
                }, { once: true });
            } catch (e) {
                console.error('Error showing modal:', e);
                alert('Lỗi hệ thống: Không thể hiển thị cửa sổ thông báo. Vui lòng thử lại sau.');
            }
        });
    });

    function loadNotifications() {
        const loadingState = document.getElementById('loadingState');
        const emptyState = document.getElementById('emptyState');
        const notificationContainer = document.getElementById('notificationContainer');
        const anchor = document.getElementById('notificationInsertAnchor');

        if (!notificationContainer || !anchor) {
            console.error('Notification container or anchor not found');
            return;
        }

        // Xóa các thông báo cũ
        const oldItems = notificationContainer.querySelectorAll('.notification-item:not(#loadingState):not(#emptyState)');
        oldItems.forEach(item => item.remove());

        const now = Date.now();
        if (cachedNotifications && (now - lastFetchTime < CACHE_DURATION)) {
            console.log('Sử dụng dữ liệu từ cache');
            if (loadingState) loadingState.classList.add('d-none');
            if (cachedNotifications.length > 0) {
                renderNotifications(cachedNotifications);
                updateUnreadCount(unreadCount);
            } else {
                if (emptyState) emptyState.classList.remove('d-none');
            }
            checkEmptyState();
            return;
        }

        if (loadingState) loadingState.classList.remove('d-none');
        if (emptyState) emptyState.classList.add('d-none');

        fetch('/acvstore/thong-bao/load?unread=true&limit=5&_=' + now)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Lỗi kết nối! Mã trạng thái: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('Dữ liệu thông báo:', data);
                if (loadingState) loadingState.classList.add('d-none');

                const unreadNotifications = Array.isArray(data.notifications)
                    ? data.notifications.filter(n => !n.daXem).slice(0, 5)
                    : [];

                cachedNotifications = unreadNotifications;
                lastFetchTime = now;
                unreadCount = data.unreadCount || 0;

                if (unreadNotifications.length > 0) {
                    renderNotifications(unreadNotifications);
                    updateUnreadCount(unreadCount);
                } else {
                    if (emptyState) emptyState.classList.remove('d-none');
                }

                checkEmptyState();
            })
            .catch(error => {
                console.error('Lỗi tải thông báo:', error);
                if (loadingState) loadingState.classList.add('d-none');
                const errorItem = document.createElement('div');
                errorItem.className = 'alert alert-danger m-3';
                errorItem.innerHTML = 'Không thể tải thông báo. Vui lòng thử lại sau.';
                anchor.before(errorItem);
            });
    }

    function loadNotificationsForTable() {
        const loadingState = document.getElementById('tableLoadingState');
        const emptyState = document.getElementById('tableEmptyState');
        const tableBody = document.getElementById('notificationTableBody');

        if (!tableBody) {
            console.error('Table body not found');
            return;
        }

        if (loadingState) loadingState.classList.remove('d-none');
        if (emptyState) emptyState.classList.add('d-none');

        const existingNotifications = tableBody.querySelectorAll('tr:not(#tableLoadingState):not(#tableEmptyState)') || [];
        existingNotifications.forEach(item => item.remove());

        fetch('/acvstore/thong-bao/load?_=' + new Date().getTime())
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Lỗi kết nối! Mã trạng thái: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('Số thông báo nhận được (table):', data.notifications ? data.notifications.length : 0);
                if (loadingState) loadingState.classList.add('d-none');
                if (data.notifications && Array.isArray(data.notifications) && data.notifications.length > 0) {
                    renderNotificationsForTable(data.notifications.slice(0, 5), false);
                    unreadCount = data.unreadCount || 0;
                    updateUnreadCount(unreadCount);
                } else {
                    if (emptyState) emptyState.classList.remove('d-none');
                }
                checkEmptyState();
            })
            .catch(error => {
                console.error('Lỗi tải thông báo cho bảng:', error);
                if (loadingState) loadingState.classList.add('d-none');
                const errorRow = document.createElement('tr');
                errorRow.innerHTML = `
                        <td colspan="6" class="text-center">
                            <div class="alert alert-danger m-3">Không thể tải thông báo. Vui lòng thử lại sau.</div>
                        </td>
                    `;
                tableBody.appendChild(errorRow);
            });
    }

    function loadNotificationsForModal(page = currentPage, size = 5) {
        const loadingState = document.getElementById('modalTableLoadingState');
        const emptyState   = document.getElementById('modalTableEmptyState');
        const tableBody    = document.getElementById('modalNotificationTableBody');
        const filterStatus = document.getElementById('filterStatus').value;
        if (!tableBody) return;

        // trạng thái đầu
        tableBody.style.minHeight = tableBody.offsetHeight + 'px';
        loadingState && loadingState.classList.remove('d-none');
        emptyState && emptyState.classList.add('d-none');

        // xoá dữ liệu cũ
        requestAnimationFrame(() => {
            const existing = tableBody.querySelectorAll('tr:not(#modalTableLoadingState):not(#modalTableEmptyState)');
            existing.forEach(row => row.remove());
        });

        const url = `/acvstore/thong-bao/xem?page=${page}&size=${size}&status=${filterStatus}&_=${Date.now()}`;
        fetch(url)
            .then(res => res.ok ? res.json() : Promise.reject(res.status))
            .then(data => {
                setTimeout(() => {
                    loadingState && loadingState.classList.add('d-none');

                    if (data.notifications?.length) {
                        // có dữ liệu -> ẩn empty row TRƯỚC khi render
                        emptyState && emptyState.classList.add('d-none');
                        renderNotificationsForTable(data.notifications, true);
                        unreadCount = data.unreadCount || 0;
                        updateUnreadCount(unreadCount);
                        totalPages = Math.ceil((data.totalCount || data.notifications.length) / size);
                        updatePagination(page);
                    } else {
                        emptyState && emptyState.classList.remove('d-none');
                        totalPages = 1;
                        updatePagination(page);
                    }

                    checkEmptyState(); // toggle lại cho chắc
                }, 200);
            })
            .catch(err => {
                console.error('Lỗi tải modal:', err);
                loadingState && loadingState.classList.add('d-none');
                tableBody.innerHTML = `
        <tr>
          <td colspan="5" class="text-center">
            <div class="alert alert-danger m-3">Không thể tải thông báo. Vui lòng thử lại sau.</div>
          </td>
        </tr>`;
            });
    }
    window.filterNotificationsByStatus = function () {
        currentPage = 0; // Reset về trang đầu khi thay đổi bộ lọc
        loadNotificationsForModal(currentPage);
    };

    function renderNotifications(notifications) {
        const notificationContainer = document.getElementById('notificationContainer');
        const anchor = document.getElementById('notificationInsertAnchor');
        const emptyState = document.getElementById('emptyState');

        if (!anchor || !notificationContainer) {
            console.error('Notification insert anchor or container not found');
            return;
        }

        const oldItems = notificationContainer.querySelectorAll('.notification-item:not(#loadingState):not(#emptyState)');
        oldItems.forEach(item => item.remove());

        if (!notifications || notifications.length === 0) {
            if (emptyState) emptyState.classList.remove('d-none');
            return;
        }

        notifications.forEach((notification) => {
            const timeAgo = formatTimeAgo(notification.thoiGian);
            const icon = '<i class="fas fa-info-circle text-primary"></i>';
            const tieuDe = notification.tieuDe || 'Không có tiêu đề';
            const noiDung = notification.noiDung || 'Không có nội dung';

            const item = document.createElement('li');
            item.className = `notification-item px-3 py-2 ${notification.daXem ? '' : 'notification-unread'}`;
            item.style.cursor = 'pointer';
            item.dataset.notificationId = notification.idChiTietThongBao;
            item.onclick = () => showConfirmModal(notification.idChiTietThongBao, item, tieuDe);

            item.innerHTML = `
                <div class="d-flex align-items-start">
                    <div class="me-2">${icon}</div>
                    <div class="flex-grow-1">
                        <div class="fw-semibold mb-1">${escapeHtml(tieuDe)}</div>
                        <div class="text-muted notification-content">${escapeHtml(noiDung)}</div>
                        <div class="text-end notification-time mt-1">${timeAgo}</div>
                    </div>
                    ${notification.daXem ? '' : '<span class="badge bg-primary ms-2">Mới</span>'}
                </div>
            `;
            anchor.before(item);
        });
    }

    function renderNotificationsForTable(notifications, isModal = false) {
        const tableBody = isModal ? document.getElementById('modalNotificationTableBody')
            : document.getElementById('notificationTableBody');
        const emptyState = isModal ? document.getElementById('modalTableEmptyState')
            : document.getElementById('tableEmptyState');
        if (!tableBody) return;

        tableBody.classList.add('opacity-50');

        // không dữ liệu -> show empty + return
        if (!Array.isArray(notifications) || notifications.length === 0) {
            emptyState && emptyState.classList.remove('d-none');
            tableBody.classList.remove('opacity-50');
            return;
        }

        // có dữ liệu -> ẩn empty trước khi render
        emptyState && emptyState.classList.add('d-none');

        // xoá cũ
        const existingRows = tableBody.querySelectorAll('tr:not(#modalTableLoadingState):not(#modalTableEmptyState)');
        existingRows.forEach(item => item.remove());

        // render
        notifications.forEach((n) => {
            const row = document.createElement('tr');
            row.className = n.daXem ? '' : 'notification-unread';
            row.dataset.notificationId = n.idChiTietThongBao;
            row.dataset.read = n.daXem ? 'true' : 'false';

            const timeAgo = formatTimeAgo(n.thoiGian);
            const tieuDe  = n.tieuDe  || 'Không có tiêu đề';
            const noiDung = n.noiDung || 'Không có nội dung';

            const actionButtons = isModal ? `
      <td class="text-center">
        <div class="d-flex gap-2 justify-content-center">
          <button class="btn btn-light" onclick="${n.daXem ? 'markAsUnread' : 'markAsRead'}(this)"
                  title="Đánh dấu ${n.daXem ? 'chưa đọc' : 'đã đọc'}">
            <i class="fas fa-${n.daXem ? 'eye-slash' : 'eye'}"></i>
          </button>
          <button class="btn btn-danger" onclick="deleteNotification(this)" title="Xoá thông báo">
            <i class="fas fa-trash-alt"></i>
          </button>
        </div>
      </td>` : '<td></td>';

            row.innerHTML = `
      <td>${escapeHtml(tieuDe)}</td>
      <td>${escapeHtml(noiDung)}</td>
      <td>${timeAgo}</td>
      <td class="status-cell"><span class="badge ${n.daXem ? 'bg-secondary' : 'bg-primary'}">${n.daXem ? 'Đã đọc' : 'Mới'}</span></td>
      ${actionButtons}
    `;

            tableBody.appendChild(row);
        });

        setTimeout(() => tableBody.classList.remove('opacity-50'), 100);
    }

    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function formatTimeAgo(thoiGian) {
        if (!thoiGian) {
            console.warn('thoiGian is null or undefined');
            return 'Không rõ thời gian';
        }
        try {
            let dateObj;
            if (typeof thoiGian === 'string') {
                if (thoiGian.includes('/')) {
                    const parts = thoiGian.split(/[/ :]/);
                    dateObj = new Date(`${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}T${parts[3].padStart(2, '0')}:${parts[4].padStart(2, '0')}:00`);
                } else {
                    dateObj = new Date(thoiGian);
                }
            } else if (typeof thoiGian === 'number') {
                dateObj = new Date(thoiGian);
            } else {
                console.warn('Invalid thoiGian format:', thoiGian);
                return 'Không rõ thời gian';
            }

            if (!dateObj || isNaN(dateObj.getTime())) {
                console.warn('Invalid date object for thoiGian:', thoiGian);
                return 'Không rõ thời gian';
            }

            const now = new Date();
            const seconds = Math.floor((now - dateObj) / 1000);
            if (seconds < 60) return 'Vừa xong';
            const minutes = Math.floor(seconds / 60);
            if (minutes < 60) return `${minutes} phút trước`;
            const hours = Math.floor(minutes / 60);
            if (hours < 24) return `${hours} giờ trước`;
            const days = Math.floor(hours / 24);
            if (days < 7) return `${days} ngày trước`;
            return dateObj.toLocaleDateString('vi-VN', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch (e) {
            console.error('Lỗi định dạng thời gian:', e, 'thoiGian:', thoiGian);
            return 'Không rõ thời gian';
        }
    }

    function showConfirmModal(notificationId, element, tieuDe = 'Thông báo') {
        console.log('Hiển thị modal xác nhận cho idChiTietThongBao:', notificationId);

        const modal = document.getElementById('confirmReadModal');
        const modalBody = modal.querySelector('.modal-body');
        const confirmButton = document.getElementById('confirmReadButton');

        if (!modal || !confirmButton) {
            console.error('Không tìm thấy modal hoặc nút xác nhận');
            return;
        }

        modalBody.textContent = `Bạn có chắc chắn muốn đánh dấu thông báo "${tieuDe}" là đã đọc?`;

        const modalInstance = new bootstrap.Modal(modal);
        modalInstance.show();

        confirmButton.onclick = () => {
            markNotificationAsRead(notificationId, element);
            modalInstance.hide();
        };
    }

    function markNotificationAsRead(notificationId, element) {
        if (!notificationId || !element) {
            console.warn('Thiếu notificationId hoặc element!');
            return;
        }

        if (markingSet.has(notificationId)) {
            console.warn('Thông báo đang được xử lý:', notificationId);
            return;
        }

        markingSet.add(notificationId);
        element.classList.add('opacity-50'); // loading effect

        fetch('/acvstore/thong-bao/danh-dau-da-xem', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: `idChiTietThongBao=${encodeURIComponent(notificationId)}`
        })
            .then(response => {
                if (!response.ok) throw new Error(`Lỗi server: ${response.status}`);
                return response.json();
            })
            .then(data => {
                // Xóa class "unread" và badge
                element.classList.remove('notification-unread');
                const badge = element.querySelector('.badge');
                if (badge) badge.remove();

                // Nếu là dòng trong bảng (modal)
                if (element.tagName === 'TR') {
                    const statusCell = element.querySelector('.status-cell');
                    if (statusCell) {
                        statusCell.innerHTML = '<span class="badge bg-secondary">Đã đọc</span>';
                    }

                    const readButton = element.querySelector('button[title*="Đánh dấu"]');
                    if (readButton) {
                        readButton.innerHTML = '<i class="fas fa-eye-slash"></i>';
                        readButton.setAttribute('onclick', 'markAsUnread(this)');
                        readButton.setAttribute('title', 'Đánh dấu chưa đọc');
                    }
                }

                // Cập nhật số thông báo chưa đọc
                unreadCount = data.unreadCount || Math.max(0, unreadCount - 1);
                updateUnreadCount(unreadCount);

                // ✅ Xoá hoàn toàn cache để dropdown lần sau tự reload
                cachedNotifications = null;

                // Làm mới các bảng hiển thị nếu cần
                if (document.getElementById('modalNotificationTableBody')) {
                    loadNotificationsForModal(currentPage);
                }
                if (document.getElementById('notificationTableBody')) {
                    loadNotificationsForTable();
                }

                // Cập nhật giao diện liên quan
                updateNotificationInLists(notificationId);
                showToast('Thông báo đã được đánh dấu là đã đọc!');
            })
            .catch(error => {
                console.error('Lỗi khi đánh dấu đã đọc:', error);
                showToast('Không thể đánh dấu thông báo đã đọc.', 'danger');
            })
            .finally(() => {
                element.classList.remove('opacity-50');
                markingSet.delete(notificationId);
            });
    }

    function markAsRead(button) {
        const row = button.closest('tr');
        const notificationId = row.dataset.notificationId;
        if (markingSet.has(notificationId)) {
            console.warn('Thông báo đã đang được xử lý:', notificationId);
            return;
        }
        markingSet.add(notificationId);
        row.classList.add('opacity-50'); // Hiệu ứng loading

        fetch('/acvstore/thong-bao/danh-dau-da-xem', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: `idChiTietThongBao=${encodeURIComponent(notificationId)}`
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Lỗi server: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                row.dataset.read = 'true';
                row.classList.remove('notification-unread');
                const badge = row.querySelector('.status-cell .badge');
                badge.className = 'badge bg-secondary';
                badge.textContent = 'Đã đọc';

                button.innerHTML = '<i class="fas fa-eye-slash"></i>';
                button.setAttribute('onclick', 'markAsUnread(this)');
                button.setAttribute('title', 'Đánh dấu chưa đọc');

                unreadCount = data.unreadCount || Math.max(0, unreadCount - 1);
                updateUnreadCount(unreadCount);

                if (cachedNotifications && Array.isArray(cachedNotifications)) {
                    cachedNotifications = cachedNotifications.filter(n => n.idChiTietThongBao !== notificationId);
                }

                // Làm mới tất cả giao diện
                if (document.getElementById('modalNotificationTableBody')) {
                    loadNotificationsForModal(currentPage);
                }
                if (document.getElementById('notificationTableBody')) {
                    loadNotificationsForTable();
                }
                if (isDropdownOpen) {
                    cachedNotifications = null; // Xóa cache
                    loadNotifications();
                }

                updateNotificationInLists(notificationId);
                showToast('Thông báo đã được đánh dấu là đã đọc!');
            })
            .catch(error => {
                console.error('Lỗi khi đánh dấu đã đọc:', error);
                showToast('Không thể đánh dấu thông báo đã đọc.', 'danger');
            })
            .finally(() => {
                row.classList.remove('opacity-50');
                markingSet.delete(notificationId);
            });
    }

    function markAsUnread(button) {
        const row = button.closest('tr');
        const notificationId = row.dataset.notificationId;
        if (markingSet.has(notificationId)) {
            console.warn('Thông báo đã đang được xử lý:', notificationId);
            return;
        }
        markingSet.add(notificationId);
        row.classList.add('opacity-50'); // Hiệu ứng loading

        fetch('/acvstore/thong-bao/danh-dau-chua-xem', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: `idChiTietThongBao=${encodeURIComponent(notificationId)}`
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Lỗi server: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                row.dataset.read = 'false';
                row.classList.add('notification-unread');
                const badge = row.querySelector('.status-cell .badge');
                badge.className = 'badge bg-primary';
                badge.textContent = 'Mới';

                button.innerHTML = '<i class="fas fa-eye"></i>';
                button.setAttribute('onclick', 'markAsRead(this)');
                button.setAttribute('title', 'Đánh dấu đã đọc');

                unreadCount = data.unreadCount || unreadCount + 1;
                updateUnreadCount(unreadCount);

                // Làm mới tất cả giao diện
                if (document.getElementById('modalNotificationTableBody')) {
                    loadNotificationsForModal(currentPage);
                }
                if (document.getElementById('notificationTableBody')) {
                    loadNotificationsForTable();
                }
                if (isDropdownOpen) {
                    cachedNotifications = null; // Xóa cache
                    loadNotifications();
                }

                showToast('Thông báo đã được đánh dấu là chưa đọc!');
            })
            .catch(error => {
                console.error('Lỗi khi đánh dấu chưa đọc:', error);
                showToast('Không thể đánh dấu thông báo chưa đọc.', 'danger');
            })
            .finally(() => {
                row.classList.remove('opacity-50');
                markingSet.delete(notificationId);
            });
    }

    function deleteNotification(button) {
        const row = button.closest('tr');
        const notificationId = row.dataset.notificationId;
        if (confirm('Bạn có chắc chắn muốn xóa thông báo này?')) {
            row.classList.add('opacity-50'); // Hiệu ứng loading
            fetch('/acvstore/thong-bao/xoa', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: `idChiTietThongBao=${encodeURIComponent(notificationId)}`
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`Lỗi server: ${response.status}`);
                    }
                    return response.json();
                })
                .then(data => {
                    row.remove();
                    if (cachedNotifications && Array.isArray(cachedNotifications)) {
                        cachedNotifications = cachedNotifications.filter(n => n.idChiTietThongBao !== notificationId);
                    }
                    unreadCount = data.unreadCount || unreadCount;
                    updateUnreadCount(unreadCount);

                    // Làm mới tất cả giao diện
                    if (document.getElementById('modalNotificationTableBody')) {
                        loadNotificationsForModal(currentPage);
                    }
                    if (document.getElementById('notificationTableBody')) {
                        loadNotificationsForTable();
                    }
                    if (isDropdownOpen) {
                        cachedNotifications = null; // Xóa cache
                        loadNotifications();
                    }

                    checkEmptyState();
                    showToast('Thông báo đã được xóa!');
                })
                .catch(error => {
                    console.error('Lỗi khi xóa thông báo:', error);
                    showToast('Không thể xóa thông báo.', 'danger');
                })
                .finally(() => {
                    row.classList.remove('opacity-50');
                });
        }
    }

    function deleteAllNotifications() {
        if (confirm('Bạn có chắc chắn muốn xóa tất cả thông báo?')) {
            fetch('/acvstore/thong-bao/xoa-tat-ca', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                }
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`Lỗi server: ${response.status}`);
                    }
                    return response.json();
                })
                .then(data => {
                    const tableBody = document.getElementById('modalNotificationTableBody');
                    const rows = tableBody.querySelectorAll('tr:not(#modalTableLoadingState):not(#modalTableEmptyState)');
                    rows.forEach(row => row.remove());
                    cachedNotifications = [];
                    unreadCount = data.unreadCount || 0;
                    updateUnreadCount(unreadCount);

                    // Làm mới tất cả giao diện
                    if (document.getElementById('modalNotificationTableBody')) {
                        loadNotificationsForModal(currentPage);
                    }
                    if (document.getElementById('notificationTableBody')) {
                        loadNotificationsForTable();
                    }
                    if (isDropdownOpen) {
                        cachedNotifications = null; // Xóa cache
                        loadNotifications();
                    }

                    checkEmptyState();
                    showToast('Tất cả thông báo đã được xóa!');
                })
                .catch(error => {
                    console.error('Lỗi khi xóa tất cả thông báo:', error);
                    showToast('Không thể xóa tất cả thông báo.', 'danger');
                });
        }
    }

    function markAllAsRead() {
        fetch('/acvstore/thong-bao/danh-dau-tat-ca', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            }
        })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(text => {
                        throw new Error(`Lỗi kết nối! Mã trạng thái: ${response.status}, Nội dung: ${text}`);
                    });
                }
                return response.json();
            })
            .then(data => {
                console.log('Đánh dấu tất cả đã đọc - Response:', data);

                // ✅ Cập nhật giao diện dropdown
                const dropdownItems = document.querySelectorAll('#notificationContainer .notification-item:not(#loadingState):not(#emptyState)');
                dropdownItems.forEach(item => {
                    item.classList.remove('notification-unread');
                    const badge = item.querySelector('.badge');
                    if (badge) badge.remove();
                });

                // ✅ Cập nhật giao diện bảng trong modal
                const modalRows = document.querySelectorAll('#modalNotificationTableBody tr:not(#modalTableLoadingState):not(#modalTableEmptyState)');
                modalRows.forEach(row => {
                    row.classList.remove('notification-unread');
                    row.dataset.read = 'true';
                    const statusCell = row.querySelector('.status-cell');
                    if (statusCell) statusCell.innerHTML = '<span class="badge bg-secondary">Đã đọc</span>';
                    const readButton = row.querySelector('button[title*="Đánh dấu"]');
                    if (readButton) {
                        readButton.innerHTML = '<i class="fas fa-eye-slash"></i>';
                        readButton.setAttribute('onclick', 'markAsUnread(this)');
                        readButton.setAttribute('title', 'Đánh dấu chưa đọc');
                    }
                });

                // ✅ Cập nhật bảng chính (nếu có)
                const tableRows = document.querySelectorAll('#notificationTableBody tr:not(#tableLoadingState):not(#tableEmptyState)');
                tableRows.forEach(row => {
                    row.classList.remove('notification-unread');
                    row.dataset.read = 'true';
                    const statusCell = row.querySelector('.status-cell');
                    if (statusCell) statusCell.innerHTML = '<span class="badge bg-secondary">Đã đọc</span>';
                });

                // ✅ Cập nhật số lượng và ẩn nút
                unreadCount = data.unreadCount || 0;
                updateUnreadCount(unreadCount);
                const markAllButtons = document.querySelectorAll('[onclick="markAllAsRead()"]');
                markAllButtons.forEach(btn => btn.style.display = unreadCount > 0 ? 'inline-block' : 'none');

                // ✅ Xóa hoàn toàn cache → dropdown sẽ fetch lại lần sau
                cachedNotifications = null;

                // ✅ Kiểm tra trạng thái rỗng
                checkEmptyState();

                // ✅ Toast
                showToast('Đã đánh dấu tất cả thông báo là đã đọc!');
            })
            .catch(error => {
                console.error('Lỗi đánh dấu tất cả thông báo đã đọc:', error);
                showToast('Không thể đánh dấu tất cả thông báo đã đọc.', 'danger');
            });
    }

    window.markAllAsRead = markAllAsRead;

    function updateNotificationInLists(notificationId) {
        const idString = notificationId.toString();
        console.log('Cập nhật giao diện cho idChiTietThongBao:', idString);

        const dropdownItems = document.querySelectorAll('#notificationContainer .notification-item');
        dropdownItems.forEach(item => {
            if (item.dataset.notificationId === idString) {
                item.remove();
            }
        });

        const tableRows = document.querySelectorAll('#notificationTableBody tr');
        tableRows.forEach(row => {
            if (row.dataset.notificationId === idString) {
                row.classList.remove('notification-unread');
                row.style.cursor = 'default';
                const statusCell = row.querySelector('.status-cell');
                if (statusCell) statusCell.innerHTML = '<span class="badge bg-secondary">Đã đọc</span>';
            }
        });

        const modalRows = document.querySelectorAll('#modalNotificationTableBody tr');
        modalRows.forEach(row => {
            if (row.dataset.notificationId === idString) {
                row.classList.remove('notification-unread');
                row.dataset.read = 'true';
                const statusCell = row.querySelector('.status-cell');
                if (statusCell) statusCell.innerHTML = '<span class="badge bg-secondary">Đã đọc</span>';
                const readButton = row.querySelector('button[title*="Đánh dấu"]');
                if (readButton) {
                    readButton.innerHTML = '<i class="fas fa-eye-slash"></i>';
                    readButton.setAttribute('onclick', 'markAsUnread(this)');
                    readButton.setAttribute('title', 'Đánh dấu chưa đọc');
                }
            }
        });

        checkEmptyState();
    }

    function updateUnreadCount(count) {
        unreadCount = count;
        const badge = document.querySelector('.notification-badge span');
        const badgeContainer = document.querySelector('.notification-badge');
        const markAllButtons = document.querySelectorAll('[onclick="markAllAsRead()"]');

        if (count > 0) {
            if (badge) badge.textContent = count;
            if (badgeContainer) badgeContainer.style.display = 'inline';
            markAllButtons.forEach(btn => btn.style.display = 'inline-block');
        } else {
            if (badgeContainer) badgeContainer.style.display = 'none';
            markAllButtons.forEach(btn => btn.style.display = 'none');
        }
    }

    function checkEmptyState() {
        // modal
        (function(){
            const body = document.getElementById('modalNotificationTableBody');
            const empty = document.getElementById('modalTableEmptyState');
            if (!body || !empty) return;
            const hasRows = body.querySelectorAll('tr:not(#modalTableLoadingState):not(#modalTableEmptyState)').length > 0;
            empty.classList.toggle('d-none', hasRows);
        })();

        // bảng chính (nếu có)
        (function(){
            const body = document.getElementById('notificationTableBody');
            const empty = document.getElementById('tableEmptyState');
            if (!body || !empty) return;
            const hasRows = body.querySelectorAll('tr:not(#tableLoadingState):not(#tableEmptyState)').length > 0;
            empty.classList.toggle('d-none', hasRows);
        })();

        // dropdown
        (function(){
            const cont = document.getElementById('notificationContainer');
            const empty = document.getElementById('emptyState');
            if (!cont || !empty) return;
            const hasItems = cont.querySelectorAll('.notification-item:not(#loadingState):not(#emptyState)').length > 0;
            empty.classList.toggle('d-none', hasItems);
        })();
    }

    function updatePagination(page) {
        const prevPage = document.getElementById('prevPage');
        const nextPage = document.getElementById('nextPage');
        const currentPageEl = document.getElementById('currentPage');

        if (currentPageEl) {
            currentPageEl.querySelector('.page-link').textContent = page + 1;
        }

        if (prevPage) {
            prevPage.classList.toggle('disabled', page === 0);
        }

        if (nextPage) {
            nextPage.classList.toggle('disabled', page >= totalPages - 1);
        }
    }

    window.changePage = function (delta) {
        const newPage = currentPage + delta;
        if (newPage >= 0 && newPage < totalPages) {
            currentPage = newPage;
            loadNotificationsForModal(currentPage);
        }
    };

    window.markAsRead = markAsRead;
    window.markAsUnread = markAsUnread;
    window.deleteNotification = deleteNotification;
    window.deleteAllNotifications = deleteAllNotifications;

    // 🔔 Bắt đầu auto refresh badge ngay khi vào trang
    startUnreadAutoRefresh();
});
