function toVND(value) {
    const n = Number(String(value).replace(/[^\d.-]/g, ""))
    return Number.isFinite(n) && n > 0 ? n.toLocaleString("vi-VN") + " VNĐ" : "Liên hệ"
}

// DOM refs
const form = document.querySelector('form[action="/search"]')
const modalForm = document.getElementById("modalSearchForm")
const searchInput = document.getElementById("searchInput")
const modalSearchInput = document.getElementById("modalSearchInput")
const searchSuggestions = document.getElementById("searchSuggestions")
const modalSearchSuggestions = document.getElementById("modalSearchSuggestions")

// Hàm xử lý tìm kiếm chung
function attachSearchHandler(inputEl, suggestionsEl, formEl) {
    if (!inputEl || !suggestionsEl) return

    let debounceTimer
    inputEl.addEventListener("input", () => {
        clearTimeout(debounceTimer)
        debounceTimer = setTimeout(async () => {
            const keyword = inputEl.value.trim()
            suggestionsEl.innerHTML = ""

            if (keyword.length < 2) {
                showSearchHistory(keyword, suggestionsEl, inputEl)
                return
            }

            try {
                const response = await fetch(`/api/search?keyword=${encodeURIComponent(keyword)}`, { credentials: "include" })
                const products = await response.json()

                suggestionsEl.innerHTML = ""
                if (!Array.isArray(products) || products.length === 0) {
                    suggestionsEl.innerHTML =
                        '<div class="suggestion-item">Không tìm thấy sản phẩm, xem các sản phẩm nổi bật</div>'
                } else {
                    products.forEach((product) => {
                        const priceText = toVND(product.price)
                        const suggestion = document.createElement("div")
                        suggestion.className = "suggestion-item"
                        suggestion.innerHTML = `
                            <img src="${product.urlHinhAnh || "/images/placeholder.png"}" alt="${product.tenSanPham}"/>
                            <div>
                                <div class="font-semibold">${product.tenSanPham}</div>
                                <div class="text-blue-800">${priceText}</div>
                            </div>`
                        suggestion.addEventListener("click", () => {
                            window.location.href = `/chitietsanpham?id=${product.id}`
                        })
                        suggestionsEl.appendChild(suggestion)
                    })
                }
                suggestionsEl.classList.add("active")
            } catch (error) {
                console.error("Error fetching search suggestions:", error)
                suggestionsEl.innerHTML = '<div class="suggestion-item">Lỗi khi tải gợi ý</div>'
                suggestionsEl.classList.add("active")
            }
        }, 300)
    })

    inputEl.addEventListener("focus", () => {
        showSearchHistory(inputEl.value.trim(), suggestionsEl, inputEl)
    })

    document.addEventListener("click", (e) => {
        if (!inputEl.contains(e.target) && !suggestionsEl.contains(e.target)) {
            suggestionsEl.classList.remove("active")
        }
    })
}

// Gắn sự kiện cho cả form desktop và modal
if (form && !form.dataset.init) {
    form.dataset.init = "1"
    attachSearchHandler(searchInput, searchSuggestions, form)

    form.addEventListener("submit", (e) => {
        const kw = (searchInput?.value || "").trim()
        if (!kw) return

        const now = Date.now()
        if (window.__lastSaveKw === kw && now - (window.__lastSaveAt || 0) < 2000) {
            return
        }
        window.__lastSaveKw = kw
        window.__lastSaveAt = now

        const url = `/api/save-search-history?keyword=${encodeURIComponent(kw)}`
        if (navigator.sendBeacon) {
            const blob = new Blob([], { type: "application/json" })
            navigator.sendBeacon(url, blob)
        } else {
            fetch(url, { method: "POST", keepalive: true, credentials: "include" }).catch(() => {})
        }
    })
}

if (modalForm && !modalForm.dataset.init) {
    modalForm.dataset.init = "1"
    attachSearchHandler(modalSearchInput, modalSearchSuggestions, modalForm)

    modalForm.addEventListener("submit", (e) => {
        const kw = (modalSearchInput?.value || "").trim()
        if (!kw) return

        const now = Date.now()
        if (window.__lastSaveKw === kw && now - (window.__lastSaveAt || 0) < 2000) {
            return
        }
        window.__lastSaveKw = kw
        window.__lastSaveAt = now

        const url = `/api/save-search-history?keyword=${encodeURIComponent(kw)}`
        if (navigator.sendBeacon) {
            const blob = new Blob([], { type: "application/json" })
            navigator.sendBeacon(url, blob)
        } else {
            fetch(url, { method: "POST", keepalive: true, credentials: "include" }).catch(() => {})
        }
    })
}

// Lịch sử tìm kiếm
async function showSearchHistory(filterKeyword = "", suggestionsEl, inputEl) {
    if (!suggestionsEl) return

    try {
        const response = await fetch("/api/search-history", { credentials: "include" })
        if (!response.ok) {
            throw new Error(
                response.status === 401 ? "Vui lòng đăng nhập để xem lịch sử tìm kiếm" : "Lỗi khi tải lịch sử tìm kiếm",
            )
        }

        const history = await response.json()
        suggestionsEl.innerHTML = ""

        const filtered = filterKeyword
            ? history.filter((kw) => kw.toLowerCase().includes(filterKeyword.toLowerCase()))
            : history

        if (!Array.isArray(filtered) || filtered.length === 0) {
            suggestionsEl.innerHTML = '<div class="history-item">Không có lịch sử tìm kiếm</div>'
        } else {
            const safe = filterKeyword.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")
            filtered.forEach((keyword) => {
                const row = document.createElement("div")
                row.className = "history-item"
                row.innerHTML = `
                    <span class="keyword">${
                    filterKeyword
                        ? keyword.replace(new RegExp(safe, "gi"), (m) => `<span class="highlight">${m}</span>`)
                        : keyword
                }</span>
                    <i class="fas fa-times delete-btn" title="Xóa mục này"></i>`

                row.querySelector(".keyword").addEventListener("click", () => {
                    inputEl.value = keyword
                    inputEl.dispatchEvent(new Event("input"))
                })

                row.querySelector(".delete-btn").addEventListener("click", async () => {
                    try {
                        const del = await fetch(`/api/search-history/delete?keyword=${encodeURIComponent(keyword)}`, {
                            method: "DELETE",
                            credentials: "include",
                        })
                        if (del.ok) {
                            showSearchHistory(filterKeyword, suggestionsEl, inputEl)
                        }
                    } catch (err) {
                        console.error("Error deleting search history item:", err)
                    }
                })

                suggestionsEl.appendChild(row)
            })
        }

        suggestionsEl.classList.add("active")
    } catch (error) {
        console.error("Error fetching search history:", error)
        suggestionsEl.innerHTML = `<div class="history-item">${error.message}</div>`
        suggestionsEl.classList.add("active")
    }
}

// Cập nhật số lượng giỏ hàng
async function updateCartCount() {
    const el = document.getElementById("cart-count")
    if (!el) return

    try {
        const response = await fetch("/api/cart", { credentials: "include" })
        if (response.ok) {
            const data = await response.json()
            el.textContent = data.chiTietGioHang ? data.chiTietGioHang.length : 0
        } else if (response.status === 401) {
            el.textContent = 0
        }
    } catch (error) {
        console.error("Error updating cart count:", error)
    }
}
document.addEventListener("DOMContentLoaded", updateCartCount)

// Tự động focus vào input trong modal khi mở
document.getElementById("searchModal")?.addEventListener("shown.bs.modal", () => {
    const modalSearchInput = document.getElementById("modalSearchInput")
    modalSearchInput?.focus()
})