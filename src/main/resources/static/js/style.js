// hoem .js


// loc san pham theo thuong hieu

document.addEventListener("DOMContentLoaded", () => {
  const brandCards = document.querySelectorAll(".brand-card");
  const productContainer = document.getElementById("product-list");

  if (!brandCards || !productContainer) {
    console.error("❌ Không tìm thấy brandCards hoặc productContainer!");
    return;
  }

  brandCards.forEach(card => {
    card.addEventListener("click", async (e) => {
      e.preventDefault();

      const brandId = card.dataset.id;
      if (!brandId) {
        console.warn("⚠️ Không có brandId trong thẻ brand-card!");
        return;
      }

      try {
        // Hiển thị loading tạm thời
        productContainer.innerHTML = `
          <div class="text-center py-5">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="mt-3">Đang tải sản phẩm...</p>
          </div>
        `;

        // Gọi đến controller trả về fragment HTML
        const response = await fetch(`/fragments/products/by-brand/${brandId}`);

        if (!response.ok) {
          throw new Error("Lỗi khi tải sản phẩm: " + response.status);
        }

        // Lấy HTML fragment trả về từ server
        const html = await response.text();

        // Thay toàn bộ nội dung danh sách sản phẩm
        productContainer.innerHTML = html;

        // Nếu bạn dùng thư viện hiệu ứng (AOS, Swiper...), gọi lại để cập nhật
        if (window.AOS) AOS.refresh();
        if (window.Swiper) {
          document.querySelectorAll('.swiper').forEach(el => new Swiper(el));
        }

      } catch (err) {
        console.error("❌ Lỗi khi lọc theo thương hiệu:", err);
        productContainer.innerHTML = `
          <p class="text-danger text-center py-4">
            Không thể tải sản phẩm. Vui lòng thử lại sau.
          </p>
        `;
      }
    });
  });
});


// product-item click navigation is handled by <a> links on product name and image

//bo loc san pham

document.addEventListener("DOMContentLoaded", () => {
  const ramSelect = document.getElementById("filter-ram");
  const cpuSelect = document.getElementById("filter-cpu");
  const colorSelect = document.getElementById("filter-color");
  const storageSelect = document.getElementById("filter-storage");
  const priceSelect = document.getElementById("filter-price");
  const applyBtn = document.getElementById("applyFilters");
  const productList = document.getElementById("product-list");

  if (!applyBtn || !productList) {
    console.error("❌ Không tìm thấy phần tử filter hoặc product list!");
    return;
  }

  applyBtn.addEventListener("click", async () => {
    const ram = ramSelect ? ramSelect.value : "";
    const cpu = cpuSelect ? cpuSelect.value : "";
    const color = colorSelect ? colorSelect.value : "";
    const storage = storageSelect ? storageSelect.value : "";
    const price = priceSelect ? priceSelect.value : "";

    let minPrice = null;
    let maxPrice = null;
    if (price) {
      const [min, max] = price.split("-");
      minPrice = min;
      maxPrice = max;
    }

    try {
      productList.innerHTML = `
        <div class="text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
          <p class="mt-3">Đang lọc sản phẩm...</p>
        </div>
      `;

      const params = new URLSearchParams();
      if (ram) params.append("ram", ram);
      if (cpu) params.append("cpu", cpu);
      if (color) params.append("color", color);
      if (storage) params.append("storage", storage);
      if (minPrice) params.append("minPrice", minPrice);
      if (maxPrice) params.append("maxPrice", maxPrice);

      // Gọi fragment HTML (sử dụng advancedSearch bên trong)
      const response = await fetch(`/fragments/products/filter?${params.toString()}`);
      if (!response.ok) throw new Error("Lỗi khi tải sản phẩm!");

      const html = await response.text();
      productList.innerHTML = html;

      if (window.AOS) AOS.refresh();
      if (window.Swiper) document.querySelectorAll('.swiper').forEach(el => new Swiper(el));

    } catch (err) {
      console.error(err);
      productList.innerHTML = `<p class="text-danger text-center py-4">
        Không thể tải kết quả lọc. Vui lòng thử lại sau.
      </p>`;
    }
  });
});

// tim kiem san pham
document.addEventListener("DOMContentLoaded", () => {
  const searchForm = document.querySelector(".search-form");
  const productContainer = document.getElementById("product-list");

  if (!searchForm || !productContainer) return;

  searchForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const q = searchForm.querySelector("input[name='q']").value.trim();
    if (!q) return;

    productContainer.innerHTML = `
      <div class="text-center py-5">
        <div class="spinner-border text-primary" role="status"></div>
        <p class="mt-3">Đang tìm kiếm sản phẩm...</p>
      </div>
    `;

    try {
      const response = await fetch(`/fragments/products/search?q=${encodeURIComponent(q)}`);
      const html = await response.text();
      productContainer.innerHTML = html;
    } catch (error) {
      console.error("❌ Lỗi khi tìm kiếm:", error);
      productContainer.innerHTML = `<p class="text-danger text-center">Không thể tải sản phẩm.</p>`;
    }
  });
});

// sắp xếp
document.addEventListener("click", function (e) {
  const btn = e.target.closest(".sort-btn");
  if (!btn) return;

  document.querySelectorAll(".sort-btn").forEach(b => b.classList.remove("active"));
  btn.classList.add("active");

  const sort = btn.getAttribute("data-sort");

  fetch(`/fragments/products/sort?sort=${sort}`)
    .then(res => res.text())
    .then(html => {
      document.querySelector("#product-list").innerHTML = html;
    })
    .catch(err => console.error("Lỗi khi tải sản phẩm:", err));
});

// =========================================================================
// QUICK SELECT VARIANT MODAL & CARD ACTIONS
// =========================================================================
document.addEventListener('DOMContentLoaded', function() {
  function formatPrice(val) {
    return new Intl.NumberFormat('vi-VN').format(val) + " ₫";
  }

  let currentProduct = null;
  let selectedVariant = null;

  // Handle Card Clicks for + Giỏ hàng AND Mua ngay (delegated on document)
  document.addEventListener('click', async function (e) {
    const cartBtn = e.target.closest('.btn-quick-add-cart');
    const buyBtn  = e.target.closest('.card-btn-buy');

    if (!cartBtn && !buyBtn) return;

    e.preventDefault();
    e.stopPropagation();

    const btn = cartBtn || buyBtn;
    const targetAction = cartBtn ? 'cart' : 'buy';
    
    // Extract productId from data-product-id or href attribute
    let productId = btn.dataset.productId;
    if (!productId && btn.getAttribute('href')) {
      const match = btn.getAttribute('href').match(/\/product\/(\d+)/);
      if (match) productId = match[1];
    }

    if (!productId) return;

    const originalText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status"></span>...';

    try {
      const res = await fetch(`/api/products/${productId}`);
      if (!res.ok) throw new Error('Product API error');
      const p = await res.json();

      btn.innerHTML = originalText;
      btn.disabled = false;

      // If product has NO variants -> direct action!
      if (!p.variants || p.variants.length === 0) {
        if (targetAction === 'cart') {
          await doQuickAddToCart(p.id, null, 1, p.name);
        } else {
          window.location.href = `/checkout/buy-now?productId=${p.id}&quantity=1`;
        }
        return;
      }

      // If product HAS variants -> open Quick Select Modal!
      const modalEl = document.getElementById('quickSelectModal');
      if (!modalEl) {
        // Fallback if modal DOM element is not present on page
        if (targetAction === 'cart') {
          await doQuickAddToCart(p.id, p.variants[0]?.id, 1, p.name);
        } else {
          let url = `/checkout/buy-now?productId=${p.id}&quantity=1`;
          if (p.variants[0]?.id) url += `&variantId=${p.variants[0].id}`;
          window.location.href = url;
        }
        return;
      }

      currentProduct = p;
      openQuickSelectModal(p, targetAction, modalEl);

    } catch (err) {
      console.error("Lỗi lấy thông tin sản phẩm:", err);
      btn.innerHTML = originalText;
      btn.disabled = false;
    }
  });

  function openQuickSelectModal(p, defaultAction, modalEl) {
    const qsModalImg       = document.getElementById('qsModalImg');
    const qsModalBrand     = document.getElementById('qsModalBrand');
    const qsModalName      = document.getElementById('qsModalName');
    const qsModalSalePrice = document.getElementById('qsModalSalePrice');
    const qsModalOldPrice  = document.getElementById('qsModalOldPrice');
    const qsModalSaleBadge = document.getElementById('qsModalSaleBadge');
    const qsColorList      = document.getElementById('qsColorList');
    const qsStorageList    = document.getElementById('qsStorageList');
    const qsQtyInput       = document.getElementById('qsQtyInput');

    if (qsModalBrand) qsModalBrand.textContent = p.brandName || 'LAPTOPSTORE';
    if (qsModalName)  qsModalName.textContent  = p.name;
    if (qsModalImg)   qsModalImg.src           = p.image || '/images/default-avatar.png';
    if (qsQtyInput)   qsQtyInput.value         = 1;

    if (qsModalSalePrice) {
      if (p.salePercent && p.salePercent > 0) {
        qsModalSalePrice.textContent = formatPrice(p.finalPrice);
        if (qsModalOldPrice) {
          qsModalOldPrice.textContent = formatPrice(p.price);
          qsModalOldPrice.style.display = 'inline';
        }
        if (qsModalSaleBadge) {
          qsModalSaleBadge.textContent = `-${p.salePercent}%`;
          qsModalSaleBadge.style.display = 'inline-block';
        }
      } else {
        qsModalSalePrice.textContent = formatPrice(p.price);
        if (qsModalOldPrice) qsModalOldPrice.style.display = 'none';
        if (qsModalSaleBadge) qsModalSaleBadge.style.display = 'none';
      }
    }

    // Render colors
    if (qsColorList) {
      qsColorList.innerHTML = '';
      const colorMap = new Map();
      p.variants.forEach(v => {
        if (v.color && !colorMap.has(v.color)) colorMap.set(v.color, v);
      });

      let firstColorBtn = null;
      colorMap.forEach((sampleVariant, colorName) => {
        const cBtn = document.createElement('button');
        cBtn.type = 'button';
        cBtn.className = 'btn btn-outline-dark color-btn';
        cBtn.style.fontSize = '0.82rem';
        cBtn.style.borderRadius = 'var(--radius-sm)';
        cBtn.textContent = colorName;
        cBtn.dataset.color = colorName;

        cBtn.addEventListener('click', () => {
          qsColorList.querySelectorAll('.color-btn').forEach(b => b.classList.remove('active'));
          cBtn.classList.add('active');
          if (sampleVariant.image && qsModalImg) qsModalImg.src = sampleVariant.image;
          renderStorageOptions(colorName);
        });

        qsColorList.appendChild(cBtn);
        if (!firstColorBtn) firstColorBtn = cBtn;
      });

      if (firstColorBtn) firstColorBtn.click();
    }

    if (window.bootstrap && bootstrap.Modal) {
      const bsModal = bootstrap.Modal.getOrCreateInstance(modalEl);
      bsModal.show();
    }
  }

  function renderStorageOptions(colorName) {
    const qsStorageList = document.getElementById('qsStorageList');
    if (!qsStorageList || !currentProduct) return;

    qsStorageList.innerHTML = '';
    const matchingVariants = currentProduct.variants.filter(v => v.color === colorName);
    let firstStorageBtn = null;

    matchingVariants.forEach((v, idx) => {
      const sBtn = document.createElement('button');
      sBtn.type = 'button';
      sBtn.className = 'btn btn-outline-dark storage-btn';
      sBtn.style.fontSize = '0.82rem';
      sBtn.style.borderRadius = 'var(--radius-sm)';
      sBtn.textContent = v.storage || 'Tiêu chuẩn';

      sBtn.addEventListener('click', () => {
        qsStorageList.querySelectorAll('.storage-btn').forEach(b => b.classList.remove('active'));
        sBtn.classList.add('active');
        selectedVariant = v;
      });

      qsStorageList.appendChild(sBtn);
      if (idx === 0) firstStorageBtn = sBtn;
    });

    if (firstStorageBtn) firstStorageBtn.click();
  }

  // Modal quantity +/- buttons
  const qsQtyDec = document.getElementById('qsQtyDec');
  const qsQtyInc = document.getElementById('qsQtyInc');
  const qsQtyInput = document.getElementById('qsQtyInput');

  if (qsQtyDec && qsQtyInput) {
    qsQtyDec.addEventListener('click', () => {
      let v = parseInt(qsQtyInput.value) || 1;
      if (v > 1) qsQtyInput.value = v - 1;
    });
  }
  if (qsQtyInc && qsQtyInput) {
    qsQtyInc.addEventListener('click', () => {
      let v = parseInt(qsQtyInput.value) || 1;
      qsQtyInput.value = v + 1;
    });
  }

  // Modal Add To Cart
  const qsBtnAddCart = document.getElementById('qsBtnAddCart');
  if (qsBtnAddCart) {
    qsBtnAddCart.addEventListener('click', async () => {
      if (!currentProduct) return;
      const qty = parseInt(qsQtyInput ? qsQtyInput.value : 1) || 1;
      const varId = selectedVariant ? selectedVariant.id : null;
      const modalEl = document.getElementById('quickSelectModal');
      if (modalEl && window.bootstrap) {
        const bsModal = bootstrap.Modal.getInstance(modalEl);
        if (bsModal) bsModal.hide();
      }
      await doQuickAddToCart(currentProduct.id, varId, qty, currentProduct.name);
    });
  }

  // Modal Buy Now
  const qsBtnBuyNow = document.getElementById('qsBtnBuyNow');
  if (qsBtnBuyNow) {
    qsBtnBuyNow.addEventListener('click', () => {
      if (!currentProduct) return;
      const qty = parseInt(qsQtyInput ? qsQtyInput.value : 1) || 1;
      const varId = selectedVariant ? selectedVariant.id : null;
      const modalEl = document.getElementById('quickSelectModal');
      if (modalEl && window.bootstrap) {
        const bsModal = bootstrap.Modal.getInstance(modalEl);
        if (bsModal) bsModal.hide();
      }
      let url = `/checkout/buy-now?productId=${currentProduct.id}&quantity=${qty}`;
      if (varId) url += `&variantId=${varId}`;
      window.location.href = url;
    });
  }

  async function doQuickAddToCart(productId, variantId, quantity, productName) {
    try {
      const payload = { productId: parseInt(productId), quantity: quantity };
      if (variantId) payload.variantId = parseInt(variantId);

      const res = await fetch('/api/cart/add', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const data = await res.json();

      if (data.redirect) {
        if (window.Swal) {
          Swal.fire({
            icon: 'warning',
            title: 'Yêu cầu đăng nhập',
            text: 'Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.',
            confirmButtonColor: '#d70018'
          }).then(() => window.location.href = data.redirect);
        } else {
          window.location.href = data.redirect;
        }
        return;
      }

      if (data.success) {
        document.querySelectorAll('.cart-badge, .cart-count').forEach(b => {
          b.textContent = data.cartCount;
          b.style.display = data.cartCount > 0 ? '' : 'none';
        });

        if (window.Swal) {
          Swal.fire({
            toast: true,
            position: 'top-end',
            icon: 'success',
            title: `Đã thêm "${productName}" vào giỏ hàng!`,
            showConfirmButton: false,
            timer: 2200
          });
        }
      } else {
        if (window.Swal) Swal.fire({ icon: 'error', title: 'Lỗi', text: data.message || 'Không thể thêm giỏ' });
      }
    } catch (err) {
      console.error("Lỗi khi thêm giỏ hàng:", err);
    }
  }
});





