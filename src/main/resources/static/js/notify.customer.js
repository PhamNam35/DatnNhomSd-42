document.addEventListener('DOMContentLoaded', () => {
    const bell       = document.getElementById('notifBell');
    const badgeEl    = document.getElementById('notifBadge');
    const listEl     = document.getElementById('notifList');
    const loadingEl  = document.getElementById('notifLoading');
    const emptyEl    = document.getElementById('notifEmpty');
    const btnMarkAll = document.getElementById('btnMarkAll');

    const modalList  = document.getElementById('modalList');
    const modalEmpty = document.getElementById('modalEmpty');
    const modalMarkAll = document.getElementById('modalMarkAll');

    let unreadCount = 0;
    let cache = null, lastFetch = 0;
    const CACHE_MS = 15000;

    // mở dropdown → fetch nếu quá hạn cache
    bell?.addEventListener('click', () => {
        const now = Date.now();
        if (!cache || now - lastFetch > CACHE_MS) fetchPanel();
        else renderPanel(cache);
    });

    // mở modal “Xem tất cả” → tải đầy đủ
    const notifModal = document.getElementById('notificationModal');
    notifModal?.addEventListener('shown.bs.modal', () => loadModal());

    // auto poll số chưa đọc
    syncUnread();
    setInterval(syncUnread, 6000);

    btnMarkAll?.addEventListener('click', markAllRead);
    modalMarkAll?.addEventListener('click', markAllRead);

    function syncUnread(){
        fetch('/acvstore/thong-bao/count',{cache:'no-store'})
            .then(r => r.ok ? r.json() : {unreadCount:0})
            .then(d => updateBadge(+d.unreadCount || 0))
            .catch(()=>{});
    }

    function fetchPanel(){
        showLoading(true);
        fetch('/acvstore/thong-bao/load?unread=false&_=' + Date.now(), {cache:'no-store'})
            .then(r => r.ok ? r.json() : Promise.reject())
            .then(data => {
                cache = (data.notifications || []).slice(0, 10);
                lastFetch = Date.now();
                unreadCount = +data.unreadCount || 0;
                renderPanel(cache);
                updateBadge(unreadCount);
            })
            .catch(() => {
                showLoading(false);
                listEl.insertAdjacentHTML('beforeend',
                    `<div class="alert alert-danger m-3">Không thể tải thông báo.</div>`);
            });
    }

    function renderPanel(items){
        listEl.querySelectorAll('.notif-item,.alert').forEach(e => e.remove());
        showLoading(false);

        if (!items || !items.length){
            emptyEl.classList.remove('d-none');
            btnMarkAll.classList.add('d-none');
            return;
        }
        emptyEl.classList.add('d-none');
        btnMarkAll.classList.toggle('d-none', (unreadCount||0) === 0);

        items.forEach(n => {
            const unread = !n.daXem;
            const time = fmtTime(n.thoiGian);
            const html = `
        <div class="notif-item" data-id="${n.idChiTietThongBao}">
          <div class="notif-thumb"><i class="fas fa-receipt"></i></div>
          <div class="notif-body">
            <p class="notif-title">${esc(n.tieuDe || 'Thông báo')}${unread ? '<span class="notif-unread-dot"></span>' : ''}</p>
            <p class="notif-preview">${esc(n.noiDung || '')}</p>
            <div class="notif-meta">
              <span class="notif-time">${time}</span>
              ${unread ? '<span class="badge bg-primary notif-badge">Mới</span>'
                : '<span class="badge bg-secondary notif-badge">Đã đọc</span>'}
            </div>
          </div>
        </div>`;
            listEl.insertAdjacentHTML('beforeend', html);
        });

        // click item → đánh dấu đã đọc
        listEl.querySelectorAll('.notif-item').forEach(it => {
            it.addEventListener('click', () => markOneRead(it.dataset.id));
        });
    }

    function loadModal(){
        modalList.innerHTML = '';
        modalEmpty.classList.add('d-none');
        modalMarkAll.classList.add('d-none');

        fetch('/acvstore/thong-bao/load?unread=false&_=' + Date.now(), {cache:'no-store'})
            .then(r => r.ok ? r.json() : Promise.reject())
            .then(data => {
                const items = data.notifications || [];
                unreadCount = +data.unreadCount || 0;
                updateBadge(unreadCount);
                if (!items.length){
                    modalEmpty.classList.remove('d-none');
                    return;
                }
                modalMarkAll.classList.toggle('d-none', unreadCount === 0);

                items.forEach(n => {
                    const unread = !n.daXem;
                    const time = fmtTime(n.thoiGian);
                    const row = document.createElement('a');
                    row.className = 'list-group-item list-group-item-action d-flex gap-3 align-items-start';
                    row.innerHTML = `
            <div class="notif-thumb"><i class="fas fa-receipt"></i></div>
            <div class="flex-grow-1">
              <div class="d-flex align-items-center justify-content-between">
                <div class="fw-semibold">${esc(n.tieuDe || 'Thông báo')}</div>
                ${unread ? '<span class="badge bg-primary">Mới</span>' : '<span class="badge bg-secondary">Đã đọc</span>'}
              </div>
              <div class="text-muted small mt-1">${esc(n.noiDung || '')}</div>
              <div class="text-muted small mt-1">${time}</div>
            </div>`;
                    row.addEventListener('click', () => markOneRead(n.idChiTietThongBao, true));
                    modalList.appendChild(row);
                });
            })
            .catch(() => {
                modalList.innerHTML = `<div class="alert alert-danger m-3">Không thể tải thông báo.</div>`;
            });
    }

    function markOneRead(id, refreshModal=false){
        if (!id) return;
        fetch('/acvstore/thong-bao/danh-dau-da-xem', {
            method:'POST',
            headers:{'Content-Type':'application/x-www-form-urlencoded'},
            body:'idChiTietThongBao=' + encodeURIComponent(id)
        })
            .then(r => r.ok ? r.json() : Promise.reject())
            .then(d => {
                unreadCount = +d.unreadCount || Math.max(0, (unreadCount||0) - 1);
                updateBadge(unreadCount);

                // update cache + UI
                if (cache){
                    cache = cache.map(x => x.idChiTietThongBao===id ? {...x, daXem:true} : x);
                    renderPanel(cache);
                } else {
                    fetchPanel();
                }
                if (refreshModal) loadModal();
            })
            .catch(()=>{});
    }

    function markAllRead(){
        fetch('/acvstore/thong-bao/danh-dau-tat-ca', {method:'POST'})
            .then(r => r.ok ? r.json() : Promise.reject())
            .then(d => {
                unreadCount = +d.unreadCount || 0;
                updateBadge(unreadCount);
                if (cache) { cache = cache.map(x => ({...x, daXem:true})); renderPanel(cache); }
                loadModal();
            })
            .catch(()=>{});
    }

    function showLoading(show){ loadingEl.classList.toggle('d-none', !show); }
    function updateBadge(count){
        unreadCount = count;
        if (!badgeEl) return;
        if (count > 0){ badgeEl.textContent = count > 99 ? '99+' : count; badgeEl.classList.remove('d-none'); }
        else { badgeEl.classList.add('d-none'); }
    }

    function esc(s){ const d=document.createElement('div'); d.textContent=s||''; return d.innerHTML; }
    function fmtTime(t){
        if (!t) return '';
        const d = (typeof t === 'string' && t.includes('/'))
            ? (()=>{ const p=t.split(/[/ :]/); return new Date(`${p[2]}-${p[1].padStart(2,'0')}-${p[0].padStart(2,'0')}T${(p[3]||'00').padStart(2,'0')}:${(p[4]||'00').padStart(2,'0')}:00`)})()
            : new Date(t);
        const diff = (Date.now() - d.getTime())/1000;
        if (diff < 60) return 'Vừa xong';
        const m = Math.floor(diff/60); if (m < 60) return `${m} phút trước`;
        const h = Math.floor(m/60);    if (h < 24) return `${h} giờ trước`;
        const day = Math.floor(h/24);  if (day < 7) return `${day} ngày trước`;
        return d.toLocaleDateString('vi-VN',{day:'2-digit',month:'2-digit',year:'numeric',hour:'2-digit',minute:'2-digit'});
    }
});
