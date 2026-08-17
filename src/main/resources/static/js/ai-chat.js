/**
 * ai-chat.js — LaptopStore AI Chatbot (v2)
 * ─────────────────────────────────────────────────────────────────
 * Tính năng:
 *   - Fetch lịch sử từ server GET /api/ai/history khi mở panel
 *   - Lưu về localStorage như bộ đệm UI (tránh lag khi mở lại)
 *   - POST /api/ai/chat để gửi câu hỏi
 *   - DELETE /api/ai/history để xóa lịch sử
 *   - Hiển thị sản phẩm dạng card với nút "Xem" và "Thêm giỏ hàng"
 *   - POST /api/cart/add để thêm vào giỏ (chatbot)
 *   - Không duplicate ID, an toàn include nhiều trang
 * ─────────────────────────────────────────────────────────────────
 */
(function () {
  'use strict';

  // ─── Guard: tránh init 2 lần ────────────────────────────────
  if (window.__aiChatInitialized) return;
  window.__aiChatInitialized = true;

  // ─── Constants ───────────────────────────────────────────────
  const LS_HISTORY_KEY = 'ts_ai_history_v2';
  const MAX_LS_ITEMS   = 50;        // giới hạn localStorage
  const DEBOUNCE_MS    = 300;       // debounce auto-resize textarea

  // ─── DOM refs ────────────────────────────────────────────────
  const panel      = document.getElementById('aiChatPanel');
  const overlay    = document.getElementById('aiPanelOverlay');
  const fabBtn     = document.getElementById('aiFabBtn');
  const fabBadge   = document.getElementById('aiFabBadge');
  const closeBtn   = document.getElementById('aiPanelClose');
  const clearBtn   = document.getElementById('aiPanelClearHistory');
  const historyBox = document.getElementById('aiChatHistoryContainer');
  const welcome    = document.getElementById('aiWelcome');
  const loadingEl  = document.getElementById('aiLoading');
  const errorBox   = document.getElementById('aiErrorBox');
  const errorMsg   = document.getElementById('aiErrorMsg');
  const textarea   = document.getElementById('aiChatInput');
  const sendBtn    = document.getElementById('aiSendBtn');
  const toast      = document.getElementById('aiToast');
  const panelBody  = panel ? panel.querySelector('.ai-panel-body') : null;

  if (!panel || !fabBtn || !textarea) {
    console.warn('[AiChat] Thiếu phần tử DOM — chatbot không khởi động.');
    return;
  }

  // ─── State ───────────────────────────────────────────────────
  let isOpen       = false;
  let isLoading    = false;
  let historyLoaded= false;

  // ─── Open / Close panel ──────────────────────────────────────
  function openPanel() {
    if (isOpen) return;
    isOpen = true;
    panel.classList.add('is-open');
    overlay.classList.add('is-visible');
    document.body.classList.add('ai-panel-open');
    if (!historyLoaded) {
      loadHistoryFromServer();
    }
    setTimeout(() => textarea.focus(), 350);
  }

  function closePanel() {
    if (!isOpen) return;
    isOpen = false;
    panel.classList.remove('is-open');
    overlay.classList.remove('is-visible');
    document.body.classList.remove('ai-panel-open');
  }

  fabBtn.addEventListener('click', openPanel);
  closeBtn.addEventListener('click', closePanel);
  overlay.addEventListener('click', closePanel);

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && isOpen) closePanel();
  });

  // ─── Load lịch sử từ server ──────────────────────────────────
  async function loadHistoryFromServer() {
    historyLoaded = true;
    setLoading(true);

    // Render từ localStorage trước (UX nhanh)
    const cached = loadFromLocalStorage();
    if (cached.length > 0) {
      historyBox.innerHTML = '';
      cached.forEach(item => renderHistoryItem(item));
      welcome.style.display = 'none';
      scrollToBottom();
    }

    try {
      const res = await fetch('/api/ai/history', {
        method: 'GET',
        headers: { 'Accept': 'application/json' }
      });

      if (!res.ok) throw new Error('HTTP ' + res.status);
      const data = await res.json();

      if (data && data.length > 0) {
        historyBox.innerHTML = '';
        welcome.style.display = 'none';

        data.forEach(item => {
          if (item.role === 'user') {
            renderUserMessage(item.message, false);
          } else if (item.role === 'assistant') {
            // Parse responseJson nếu có
            if (item.responseJson) {
              try {
                const resp = JSON.parse(item.responseJson);
                renderAssistantResponse(resp, false);
              } catch {
                renderAssistantText(item.message, false);
              }
            } else {
              renderAssistantText(item.message, false);
            }
          }
        });

        // Lưu vào localStorage
        saveToLocalStorage(data);
        scrollToBottom();
      }
    } catch (err) {
      console.warn('[AiChat] Không load được lịch sử từ server:', err.message);
      // fallback localStorage đã render rồi
    } finally {
      setLoading(false);
    }
  }

  // Render 1 item history (dùng khi reload từ server)
  function renderHistoryItem(item) {
    if (item.role === 'user') {
      renderUserMessage(item.message, false);
    } else if (item.role === 'assistant') {
      if (item.responseJson) {
        try {
          renderAssistantResponse(JSON.parse(item.responseJson), false);
        } catch {
          renderAssistantText(item.message, false);
        }
      } else {
        renderAssistantText(item.message, false);
      }
    }
  }

  // ─── Send message ────────────────────────────────────────────
  async function sendMessage() {
    const text = textarea.value.trim();
    if (!text || isLoading) return;

    textarea.value = '';
    resizeTextarea();

    // Ẩn welcome
    welcome.style.display = 'none';

    // Render user bubble
    renderUserMessage(text, true);
    scrollToBottom();

    setLoading(true);
    hideError();

    try {
      const res = await fetch('/api/ai/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({ message: text, topK: 5 })
      });

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.error || 'Lỗi kết nối server (HTTP ' + res.status + ')');
      }

      const data = await res.json();
      renderAssistantResponse(data, true);
      scrollToBottom();

    } catch (err) {
      showError(err.message || 'Có lỗi xảy ra, vui lòng thử lại!');
    } finally {
      setLoading(false);
    }
  }

  sendBtn.addEventListener('click', sendMessage);
  textarea.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  });

  // Suggestion chips
  document.addEventListener('click', (e) => {
    const chip = e.target.closest('.ai-suggestion-chip');
    if (!chip) return;
    textarea.value = chip.dataset.text || chip.textContent.trim();
    resizeTextarea();
    sendMessage();
  });

  // ─── Render functions ────────────────────────────────────────

  function renderUserMessage(text, animate) {
    const div = document.createElement('div');
    div.className = 'ai-msg ai-msg--user' + (animate ? ' ai-msg--animate' : '');
    div.innerHTML = `<div class="ai-bubble ai-bubble--user">${escapeHtml(text)}</div>`;
    historyBox.appendChild(div);
  }

  function renderAssistantText(text, animate) {
    const div = document.createElement('div');
    div.className = 'ai-msg ai-msg--bot' + (animate ? ' ai-msg--animate' : '');
    div.innerHTML = `
      <div class="ai-avatar-mini">🤖</div>
      <div class="ai-bubble ai-bubble--bot">${escapeHtml(text)}</div>`;
    historyBox.appendChild(div);
  }

  function renderAssistantResponse(data, animate) {
    if (!data) return;

    const div = document.createElement('div');
    div.className = 'ai-msg ai-msg--bot' + (animate ? ' ai-msg--animate' : '');

    // Tính confidence
    const confidence = data.confidenceScore || 0;
    const stars = buildConfidenceBar(confidence);

    // Phần answer text
    let html = `
      <div class="ai-avatar-mini">🤖</div>
      <div class="ai-bubble ai-bubble--bot">
        <div class="ai-answer-text">${formatAnswer(data.answer || '')}</div>`;

    // Confidence
    if (data.confidenceScore !== undefined) {
      html += `<div class="ai-confidence">${stars} <span>${Math.round(confidence * 100)}%</span></div>`;
    }

    // Recommended products — luôn dùng card grid
    if (data.recommendedProducts && data.recommendedProducts.length > 0) {
      html += `<div class="ai-products-label">💡 Sản phẩm gợi ý:</div>
               <div class="ai-product-grid">`;
      data.recommendedProducts.forEach(p => { html += buildProductCard(p); });
      html += `</div>`;
    }

    // Citations
    if (data.citations && data.citations.length > 0) {
      const cList = data.citations.map(c => `<li>${escapeHtml(c)}</li>`).join('');
      html += `<details class="ai-citations"><summary>📚 Nguồn tham khảo</summary><ul>${cList}</ul></details>`;
    }

    html += `</div>`;
    div.innerHTML = html;
    historyBox.appendChild(div);

    // Attach add-to-cart handlers
    div.querySelectorAll('.ai-add-cart-btn').forEach(btn => {
      btn.addEventListener('click', () => addToCart(btn));
    });
  }

  // Bảng so sánh sản phẩm (dùng khi có ≥2 sản phẩm)
  function buildProductTable(products) {
    const rows = products.map(p => {
      const productId   = p.productId || p.id || '';
      const name        = escapeHtml(p.productName || p.name || 'Sản phẩm');
      const brand       = escapeHtml(p.brandName || p.brand || '—');
      const price       = p.price       ? formatPrice(p.price)       : '';
      const salePrice   = p.salePrice   ? formatPrice(p.salePrice)   : '';
      const salePercent = p.salePercent ? p.salePercent               : '';
      const imageUrl    = escapeHtml(p.imageUrl || p.image || '/images/products/placeholder.png');
      const url         = p.url || (productId ? `/product/${productId}` : '#');
      const displayPrice = salePrice
        ? `<span class="ai-tbl-sale">${salePrice}</span><br><span class="ai-tbl-old">${price}</span>`
        : `<span class="ai-product-price">${price}</span>`;
      const badge = salePercent ? `<span class="ai-product-badge ai-badge-inline">-${salePercent}%</span>` : '';

      return `<tr class="ai-tbl-row">
        <td class="ai-tbl-img-cell">
          <a href="${url}" target="_blank" rel="noopener">
            <img src="${imageUrl}" alt="${name}" class="ai-tbl-img"
                 onerror="this.src='/images/products/placeholder.png'">
          </a>
        </td>
        <td class="ai-tbl-name-cell">
          ${badge}
          <a href="${url}" class="ai-tbl-name-link" target="_blank" rel="noopener">${name}</a>
          <div class="ai-tbl-brand">${brand}</div>
        </td>
        <td class="ai-tbl-price-cell">${displayPrice}</td>
        <td class="ai-tbl-action-cell">
          <a href="${url}" class="ai-btn ai-btn--view ai-btn--sm" target="_blank" rel="noopener">
            <i class="bi bi-eye"></i> Xem
          </a>
          ${productId ? `<button class="ai-btn ai-btn--cart ai-btn--sm ai-add-cart-btn"
                  data-product-id="${productId}"
                  data-product-name="${name}">
            <i class="bi bi-cart-plus"></i> Giỏ hàng
          </button>` : ''}
        </td>
      </tr>`;
    }).join('');

    return `<div class="ai-product-table-wrap">
      <table class="ai-product-table">
        <thead>
          <tr>
            <th class="ai-th-img"></th>
            <th class="ai-th-name">Sản phẩm</th>
            <th class="ai-th-price">Giá</th>
            <th class="ai-th-action">Thao tác</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>
    </div>`;
  }

  function buildConfidenceBar(score) {
    const pct = Math.round(score * 100);
    const color = pct >= 70 ? '#22c55e' : pct >= 40 ? '#f59e0b' : '#ef4444';
    return `<div class="ai-conf-bar" title="Độ tin cậy ${pct}%">
              <div class="ai-conf-fill" style="width:${pct}%;background:${color}"></div>
            </div>`;
  }

  function buildProductCard(p) {
    const productId  = p.productId || p.id || '';
    const name       = escapeHtml(p.productName || p.name || 'Sản phẩm');
    const brand      = escapeHtml(p.brandName || p.brand || '');
    const price      = p.price ? formatPrice(p.price) : '';
    const imageUrl   = escapeHtml(p.imageUrl || p.image || '/images/products/placeholder.png');
    const url        = p.url || (productId ? `/product/${productId}` : '#');
    const salePrice  = p.salePrice ? formatPrice(p.salePrice) : '';
    const salePercent = p.salePercent ? p.salePercent : '';

    return `
      <div class="ai-product-card">
        <a href="${url}" class="ai-product-img-link" target="_blank" rel="noopener">
          <img src="${imageUrl}" alt="${name}" class="ai-product-img" loading="lazy"
               onerror="this.src='/images/products/placeholder.png'">
          ${salePercent ? `<span class="ai-product-badge">-${salePercent}%</span>` : ''}
        </a>
        <div class="ai-product-info">
          ${brand ? `<div class="ai-product-brand">${brand}</div>` : ''}
          <div class="ai-product-name">
            <a href="${url}" target="_blank" rel="noopener">${name}</a>
          </div>
          <div class="ai-product-price-row">
            ${salePrice ? `<span class="ai-product-sale-price">${salePrice}</span>` : ''}
            ${price ? `<span class="ai-product-price ${salePrice ? 'is-old' : ''}">${price}</span>` : ''}
          </div>
          <div class="ai-product-actions">
            <a href="${url}" class="ai-btn ai-btn--view" target="_blank" rel="noopener">
              <i class="bi bi-eye"></i> Xem
            </a>
            ${productId ? `
            <button class="ai-btn ai-btn--cart ai-add-cart-btn"
                    data-product-id="${productId}"
                    data-product-name="${name}">
              <i class="bi bi-cart-plus"></i> Giỏ hàng
            </button>` : ''}
          </div>
        </div>
      </div>`;
  }

  // ─── Add to cart ─────────────────────────────────────────────
  async function addToCart(btn) {
    const productId = btn.dataset.productId;
    const productName = btn.dataset.productName;
    if (!productId || btn.disabled) return;

    btn.disabled = true;
    btn.innerHTML = '<span class="ai-spinner-sm"></span> Đang thêm...';

    try {
      const res = await fetch('/api/cart/add', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ productId, quantity: 1 })
      });
      const data = await res.json();

      if (data.redirect) {
        showToast('⚠️ Vui lòng đăng nhập để thêm vào giỏ hàng', 'warning');
        setTimeout(() => window.location.href = data.redirect, 1200);
        btn.innerHTML = '<i class="bi bi-cart-plus"></i> Giỏ hàng';
        btn.disabled = false;
        return;
      }

      if (data.success) {
        btn.innerHTML = '<i class="bi bi-check-circle-fill"></i> Đã thêm!';
        btn.classList.add('ai-btn--success');
        showToast(`✅ Đã thêm "${productName}" vào giỏ hàng!`, 'success');
        updateCartBadgeInHeader(data.cartCount);
      } else {
        btn.innerHTML = '<i class="bi bi-cart-plus"></i> Giỏ hàng';
        btn.disabled = false;
        showToast('❌ ' + (data.message || 'Không thể thêm vào giỏ'), 'error');
      }
    } catch (err) {
      btn.innerHTML = '<i class="bi bi-cart-plus"></i> Giỏ hàng';
      btn.disabled = false;
      showToast('❌ Lỗi kết nối, thử lại sau!', 'error');
    }
  }

  // Cập nhật badge số lượng giỏ hàng trên header
  function updateCartBadgeInHeader(count) {
    const cartBadges = document.querySelectorAll('.cart-count, .cart-badge, [data-cart-count]');
    cartBadges.forEach(el => {
      el.textContent = count;
      el.style.display = count > 0 ? '' : 'none';
    });
    // Badge trên FAB
    if (fabBadge && count > 0) {
      fabBadge.textContent = '';
      fabBadge.classList.add('has-notification');
    }
  }

  // ─── Clear history ───────────────────────────────────────────
  clearBtn.addEventListener('click', async () => {
    if (!confirm('Xóa toàn bộ lịch sử chat?')) return;

    try {
      await fetch('/api/ai/history', { method: 'DELETE' });
    } catch (_) {}

    historyBox.innerHTML = '';
    clearLocalStorage();
    welcome.style.display = '';
    hideError();
    showToast('🗑️ Đã xóa lịch sử chat', 'info');
  });

  // ─── Helper: loading state ───────────────────────────────────
  function setLoading(state) {
    isLoading = state;
    loadingEl.style.display = state ? 'flex' : 'none';
    sendBtn.disabled = state;
    textarea.disabled = state;
  }

  function showError(msg) {
    errorBox.style.display = 'flex';
    errorMsg.textContent = msg;
  }

  function hideError() {
    errorBox.style.display = 'none';
  }

  // ─── Helper: localStorage ────────────────────────────────────
  function saveToLocalStorage(items) {
    try {
      const slim = items.slice(-MAX_LS_ITEMS);
      localStorage.setItem(LS_HISTORY_KEY, JSON.stringify(slim));
    } catch (_) {}
  }

  function loadFromLocalStorage() {
    try {
      return JSON.parse(localStorage.getItem(LS_HISTORY_KEY) || '[]');
    } catch {
      return [];
    }
  }

  function clearLocalStorage() {
    try { localStorage.removeItem(LS_HISTORY_KEY); } catch (_) {}
  }

  // ─── Helper: auto-resize textarea ────────────────────────────
  let resizeTimer;
  function resizeTextarea() {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => {
      textarea.style.height = 'auto';
      textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px';
    }, DEBOUNCE_MS);
  }
  textarea.addEventListener('input', resizeTextarea);

  // ─── Helper: scroll to bottom ────────────────────────────────
  function scrollToBottom() {
    if (panelBody) {
      requestAnimationFrame(() => {
        panelBody.scrollTop = panelBody.scrollHeight;
      });
    }
  }

  // ─── Helper: format answer text (smart markdown + plain-text detection) ───
  function formatAnswer(text) {
    if (!text) return '';

    // Chuẩn hoá newline: \n literal (từ JSON) hoặc ký tự thực
    let normalized = text
      .replace(/\\n/g, '\n')   // JSON escaped newline
      .replace(/\r\n/g, '\n')
      .replace(/\r/g, '\n');

    // ── Tách inline numbered lists dạng "1. Tiêu đề:...  2. Tiêu đề:" ──
    // Tách khi có số đứng sau dấu hai chấm, khoảng trắng hoặc đầu câu
    normalized = normalized
      .replace(/(?::\s*|\s+)((?:\d+)\.\s+(?=[A-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠƯẠ-ỹ]))/g, '\n$1')
      .replace(/(\.\s+)(Tóm lại|Kết luận|Nhận xét|Gợi ý|Lưu ý)(:|\s)/gi, '.\n\n$2$3');

    const lines = normalized.split('\n');
    let html = '';
    let inList = false;
    let inOl = false;
    let inSummary = false;

    const flushList = () => {
      if (inList)  { html += '</ul>'; inList = false; }
      if (inOl)    { html += '</ol>'; inOl   = false; }
    };

    for (let i = 0; i < lines.length; i++) {
      const raw = lines[i];
      const trimmed = raw.trim();

      // ── Dòng trống ──
      if (!trimmed) {
        flushList();
        html += '<div class="ai-ans-spacer"></div>';
        continue;
      }

      // ── Header ## / ### ──
      if (/^###\s+/.test(raw)) {
        flushList();
        html += `<div class="ai-ans-h3">${applyInlineFormat(escapeHtml(raw.replace(/^###\s+/, '')))}</div>`;
        continue;
      }
      if (/^##\s+/.test(raw)) {
        flushList();
        html += `<div class="ai-ans-h2">${applyInlineFormat(escapeHtml(raw.replace(/^##\s+/, '')))}</div>`;
        continue;
      }

      // ── Bullet list (-/•/*) ──
      if (/^[-•*]\s+/.test(raw)) {
        flushList();
        if (!inList) { html += '<ul class="ai-ans-list">'; inList = true; }
        html += `<li>${applyInlineFormat(escapeHtml(trimmed.replace(/^[-•*]\s+/, '')))}</li>`;
        continue;
      }

      // ── Numbered list với nội dung dài (1. Tên: mô tả) ──
      const numMatch = trimmed.match(/^(\d+)\.\s+(.+)$/s);
      if (numMatch) {
        flushList();
        const num = numMatch[1];
        const content = numMatch[2];

        // Tách tên sản phẩm (phần trước dấu ':' hoặc trước phần mô tả/ID)
        let splitAt = -1;
        const colonIdx = content.indexOf(':');
        if (colonIdx > 0 && colonIdx < 70) {
          splitAt = colonIdx;
        } else {
          // Tách tại đóng ngoặc (ID #X) nếu không có dấu ':'
          const idMatch = content.match(/^([^(]+\(ID\s*#[0-9]+\))\s*(.*)$/i);
          if (idMatch) {
            splitAt = idMatch[1].length;
          }
        }

        if (splitAt > 0) {
          // Có tên + mô tả rõ ràng
          let titleStr = content.slice(0, splitAt).trim();
          let descStr  = content.slice(splitAt).replace(/^[:\s-]+/, '').trim();

          const title = applyInlineFormat(escapeHtml(titleStr));
          const desc  = applyInlineFormat(escapeHtml(descStr));

          html += `<div class="ai-ans-num-block">
            <div class="ai-ans-num-title">
              <span class="ai-ans-num-badge">${num}</span>
              ${title}
            </div>
            ${desc ? `<div class="ai-ans-num-desc">${desc}</div>` : ''}
          </div>`;
        } else {
          // Numbered list bình thường
          if (!inOl) { html += '<ol class="ai-ans-list">'; inOl = true; }
          html += `<li>${applyInlineFormat(escapeHtml(content))}</li>`;
        }
        continue;
      }

      // ── Block tóm lại / kết luận ──
      if (/^(tóm lại|kết luận|nhận xét|lưu ý|gợi ý)/i.test(trimmed)) {
        flushList();
        const colonPos = trimmed.indexOf(':');
        const label   = colonPos > 0 ? escapeHtml(trimmed.slice(0, colonPos)) : 'Tóm lại';
        const body    = colonPos > 0 ? applyInlineFormat(escapeHtml(trimmed.slice(colonPos + 1).trim())) : '';
        html += `<div class="ai-ans-summary">
          <div class="ai-ans-summary-label">💡 ${label}</div>
          ${body ? `<div class="ai-ans-summary-body">${body}</div>` : ''}
        </div>`;
        continue;
      }

      // ── Dòng thường ──
      flushList();
      html += `<div class="ai-ans-line">${applyInlineFormat(escapeHtml(trimmed))}</div>`;
    }

    flushList();
    return html;
  }

  // Inline formatting: **bold**, *italic*, `code`
  function applyInlineFormat(str) {
    return str
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/`([^`]+)`/g, '<code class="ai-ans-code">$1</code>');
  }

  // ─── Helper: format price ────────────────────────────────────
  function formatPrice(price) {
    const n = typeof price === 'number' ? price : parseFloat(price);
    if (isNaN(n)) return '';
    return n.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' });
  }

  // ─── Helper: escape HTML ─────────────────────────────────────
  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  // ─── Toast notification ──────────────────────────────────────
  let toastTimer;
  function showToast(msg, type = 'info') {
    toast.textContent = msg;
    toast.className = 'ai-toast ai-toast--' + type + ' ai-toast--visible';
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      toast.classList.remove('ai-toast--visible');
    }, 3000);
  }

  // ─── Init: show dot khi có lịch sử localStorage ──────────────
  if (loadFromLocalStorage().length > 0) {
    if (fabBadge) {
      fabBadge.textContent = '';
      fabBadge.classList.add('has-notification');
    }
  }

})();
