package com.laptopstore.laptopstore;

import com.laptopstore.laptopstore.Controller.AdminOrderController;
import com.laptopstore.laptopstore.Controller.AdminProductController;
import com.laptopstore.laptopstore.Controller.ProductApiController;
import com.laptopstore.laptopstore.Repository.CategoryRepository;
import com.laptopstore.laptopstore.Repository.ProductRepository;
import com.laptopstore.laptopstore.Service.*;
import com.laptopstore.laptopstore.entity.Brand;
import com.laptopstore.laptopstore.entity.Order;
import com.laptopstore.laptopstore.entity.Product;
import com.laptopstore.laptopstore.enums.OrderStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductManualIdAndSearchTest {

    @Mock private ProductService productService;
    @Mock private BrandService brandService;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductVariantService variantService;
    @Mock private RagIntegrationService ragIntegrationService;
    @Mock private OrderService orderService;
    @Mock private HttpServletRequest request;

    @InjectMocks private AdminProductController adminProductController;
    @InjectMocks private ProductApiController productApiController;
    @InjectMocks private AdminOrderController adminOrderController;

    private Brand brand;

    @BeforeEach
    void setUp() {
        brand = new Brand();
        brand.setId(1L);
        brand.setName("ASUS");
    }

    @Test
    @DisplayName("Product.getDisplayId ưu tiên productCode nhập thủ công và fallback về id")
    void testGetDisplayId() {
        Product p = new Product();
        p.setId(10L);
        assertEquals("10", p.getDisplayId());

        p.setProductCode("SN-ASUS-2026-001");
        assertEquals("SN-ASUS-2026-001", p.getDisplayId());
    }

    @Test
    @DisplayName("Thêm sản phẩm: từ chối khi để trống mã ID xuất xưởng")
    void testAddProduct_BlankProductCode_Rejected() throws Exception {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = adminProductController.addProduct(
                "   ", "ASUS TUF A15", "FA506", BigDecimal.valueOf(25000000), 1, 1L,
                null, null, null, null, null, null, null, null, null, null, 0,
                null, null, redirect, request
        );

        assertEquals("redirect:/admin/products", view);
        assertTrue(redirect.getFlashAttributes().containsKey("errorMessage"));
        verify(productService, never()).save(any());
    }

    @Test
    @DisplayName("Thêm sản phẩm: từ chối khi trùng mã ID xuất xưởng")
    void testAddProduct_DuplicateProductCode_Rejected() throws Exception {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(brandService.getById(1L)).thenReturn(Optional.of(brand));
        when(productService.existsByProductCode("SN-001", null)).thenReturn(true);

        String view = adminProductController.addProduct(
                "SN-001", "ASUS TUF A15", "FA506", BigDecimal.valueOf(25000000), 1, 1L,
                null, null, null, null, null, null, null, null, null, null, 0,
                null, null, redirect, request
        );

        assertEquals("redirect:/admin/products", view);
        String msg = (String) redirect.getFlashAttributes().get("errorMessage");
        assertNotNull(msg);
        assertTrue(msg.contains("SN-001"));
        verify(productService, never()).save(any());
    }

    @Test
    @DisplayName("Thêm sản phẩm: cho phép thêm 2 máy cùng Model khi khác mã ID xuất xưởng")
    void testAddProduct_DifferentProductCodeSameModel_Success() throws Exception {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(brandService.getById(1L)).thenReturn(Optional.of(brand));
        when(productService.existsByProductCode("SN-002", null)).thenReturn(false);
        when(productService.save(any(Product.class))).thenAnswer(inv -> {
            Product saved = inv.getArgument(0);
            saved.setId(22L);
            return saved;
        });
        when(productService.getProductById(22L)).thenAnswer(inv -> {
            Product p = new Product();
            p.setId(22L);
            p.setProductCode("SN-002");
            p.setName("ASUS TUF A15");
            p.setModel("FA506");
            return Optional.of(p);
        });

        String view = adminProductController.addProduct(
                "  SN-002  ", "ASUS TUF A15", "FA506", BigDecimal.valueOf(25000000), 1, 1L,
                null, null, null, null, null, null, null, null, null, null, 0,
                null, null, redirect, request
        );

        assertEquals("redirect:/admin/products", view);
        assertTrue(redirect.getFlashAttributes().containsKey("successMessage"));
        verify(productService).releaseDeletedProductCode("SN-002");
        verify(productService).save(argThat(p -> "SN-002".equals(p.getProductCode()) && "FA506".equals(p.getModel())));
    }

    @Test
    @DisplayName("Tìm kiếm Admin: nhập 1 phần nội dung số vẫn tìm theo partial search thay vì exact ID")
    void testAdminProductSearch_PartialNumericQuery() {
        Product p1 = new Product();
        p1.setId(1L);
        p1.setProductCode("ANV15-01");
        p1.setName("Acer Nitro 15");
        p1.setModel("ANV15");

        when(productService.searchByName("15")).thenReturn(List.of(p1));
        when(productService.countLowStockProducts(5)).thenReturn(0L);
        when(brandService.getAllBrands()).thenReturn(List.of(brand));
        when(categoryRepository.findAll()).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = adminProductController.listProducts("15", model);

        assertEquals("admin/products", view);
        assertEquals(List.of(p1), model.getAttribute("products"));
        verify(productService).searchByName("15");
        verify(productService, never()).getProductById(anyLong());
    }

    @Test
    @DisplayName("API gợi ý sản phẩm: trả về gợi ý khi nhập 1 phần từ khóa")
    void testSuggestProductsApi_PartialKeyword() {
        Product p = new Product();
        p.setId(5L);
        p.setProductCode("LP-DELL-5430");
        p.setName("Dell Inspiron 14 5430");
        p.setModel("INSP-5430");
        p.setBrand(brand);
        p.setPrice(BigDecimal.valueOf(19000000));
        p.setSalePercent(10);
        p.setStock(5);
        p.setCpu("Core i5");
        p.setRam("16GB");

        when(productService.searchByName("5430")).thenReturn(List.of(p));

        ResponseEntity<List<Map<String, Object>>> response = productApiController.suggestProducts("5430", 8);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        Map<String, Object> item = response.getBody().get(0);
        assertEquals("LP-DELL-5430", item.get("productCode"));
        assertEquals("INSP-5430", item.get("model"));
        assertEquals("Dell Inspiron 14 5430", item.get("name"));

        ResponseEntity<List<Map<String, Object>>> emptyResp = productApiController.suggestProducts("   ", 8);
        assertEquals(200, emptyResp.getStatusCode().value());
        assertTrue(emptyResp.getBody().isEmpty());
    }

    @Test
    @DisplayName("Tìm kiếm & gợi ý đơn hàng Admin: hỗ trợ tìm 1 phần nội dung")
    void testAdminOrderSearchAndSuggest_PartialQuery() {
        Order order = new Order();
        order.setId(42L);
        order.setOrderCode("DH20261118-0042");
        order.setReceiverName("Nguyen Van Minh");
        order.setReceiverPhone("0912345678");
        order.setTotalAmount(BigDecimal.valueOf(25000000));
        order.setOrderStatus(OrderStatus.DA_XAC_NHAN);
        order.setCreatedAt(LocalDateTime.now());

        when(orderService.searchOrders("Minh")).thenReturn(List.of(order));

        Model model = new ConcurrentModel();
        String view = adminOrderController.searchOrders("Minh", model);
        assertEquals("admin/orders", view);
        assertEquals(List.of(order), model.getAttribute("orders"));

        List<Map<String, Object>> suggestions = adminOrderController.suggestOrders("Minh", 8);
        assertEquals(1, suggestions.size());
        assertEquals("DH20261118-0042", suggestions.get(0).get("orderCode"));
        assertEquals("Nguyen Van Minh", suggestions.get(0).get("receiverName"));
    }
}
