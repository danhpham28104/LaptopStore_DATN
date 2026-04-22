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


// click san pham xem chi tiet
document.addEventListener('DOMContentLoaded', function() {
  document.querySelectorAll('.product-item').forEach(item => {
    item.addEventListener('click', function(e) {
      if (e.target.closest('.cart-btn') || e.target.closest('.buy-now')) return;
      const productLink = this.querySelector('.product-name a');
      if (productLink) window.location.href = productLink.href;
    });
  });
});

//bo loc san pham

document.addEventListener("DOMContentLoaded", () => {
  const ramSelect = document.getElementById("filter-ram");
  const cpuSelect = document.getElementById("filter-cpu");
  const storageSelect = document.getElementById("filter-storage");
  const priceSelect = document.getElementById("filter-price");
  const applyBtn = document.getElementById("applyFilters");
  const productList = document.getElementById("product-list");

  if (!applyBtn || !productList) {
    console.error("❌ Không tìm thấy phần tử filter hoặc product list!");
    return;
  }

  applyBtn.addEventListener("click", async () => {
    const ram = ramSelect.value;
    const cpu = cpuSelect ? cpuSelect.value : "";
    const storage = storageSelect.value;
    const price = priceSelect.value;

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

//săp xếp


document.addEventListener("click", function (e) {
  const btn = e.target.closest(".sort-btn");
  if (!btn) return; // nếu click không phải vào nút sort

  // Bỏ active cũ
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



