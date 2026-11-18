package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.*;
import com.laptopstore.laptopstore.entity.*;
import com.laptopstore.laptopstore.enums.OrderStatus;
import com.laptopstore.laptopstore.enums.PaymentMethod;
import com.laptopstore.laptopstore.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private VoucherRepository voucherRepository;
    @Mock private VoucherUsageRepository voucherUsageRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private StockLogService stockLogService;
    @Mock private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Product product;
    private ProductVariant variant;
    private Cart cart;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        product = new Product();
        product.setId(100L);
        product.setName("Laptop Gaming ASUS ROG");
        product.setPrice(BigDecimal.valueOf(20000000));
        product.setStock(10);
        product.setReservedStock(0);

        variant = new ProductVariant();
        variant.setId(200L);
        variant.setColor("Black");
        variant.setStorage("512GB SSD");
        variant.setStock(5);
        variant.setReservedStock(0);
        variant.setProduct(product);

        cart = new Cart();
        cart.setId(1L);
        cart.setUser(user);
        cart.setItems(new ArrayList<>());
    }

    @Test
    @DisplayName("Tạo đơn hàng từ giỏ hàng (COD) -> CHO_XAC_NHAN, CHƯA trừ kho")
    void testCreateOrderFromCart_COD_Success_DoesNotDeductStock() {
        CartItem cartItem = new CartItem();
        cartItem.setProduct(product);
        cartItem.setVariant(variant);
        cartItem.setQuantity(2);
        cartItem.setUnitPriceAtAdd(BigDecimal.valueOf(20000000));
        cart.getItems().add(cartItem);

        when(cartRepository.findByUser_Id(1L)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order order = orderService.createOrderFromCart(
                user, "Nguyen Van A", "123 Le Loi", "0901234567",
                PaymentMethod.COD, null
        );

        assertNotNull(order);
        assertEquals(OrderStatus.CHO_XAC_NHAN, order.getOrderStatus());
        assertFalse(order.isInventoryDeducted());
        assertEquals("Nguyen Van A", order.getReceiverName());
        assertEquals(BigDecimal.valueOf(40000000), order.getTotalAmount());
        assertEquals(5, variant.getStock()); // Tồn kho chưa bị trừ khi vừa tạo đơn
        assertEquals(10, product.getStock());
        verify(productVariantRepository, never()).save(any());
        verify(productRepository, never()).save(any());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Tạo đơn hàng Mua Ngay với QR Code -> CHO_THANH_TOAN, CHƯA trừ kho")
    void testCreateOrderInstant_QR_SetsPendingPaymentWithoutDeductingStock() {
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order order = orderService.createOrderInstant(
                user, product, variant, 1,
                "Tran Van B", "456 Nguyen Hue", "0987654321",
                PaymentMethod.QR_CODE, null
        );

        assertNotNull(order);
        assertEquals(OrderStatus.CHO_THANH_TOAN, order.getOrderStatus());
        assertFalse(order.isInventoryDeducted());
        assertNotNull(order.getPaymentDeadline());
        assertEquals(0, variant.getReservedStock());
        assertEquals(5, variant.getStock());
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Áp dụng mã Voucher phần trăm có giới hạn giảm tối đa (Max Discount)")
    void testApplyVoucher_PercentageWithMaxLimit() {
        Voucher voucher = new Voucher();
        voucher.setId(1L);
        voucher.setCode("SALE50");
        voucher.setDiscountType("PERCENT");
        voucher.setDiscountValue(BigDecimal.valueOf(50)); // Giảm 50%
        voucher.setMaxDiscountAmount(BigDecimal.valueOf(1000000)); // Tối đa 1 triệu
        voucher.setActive(true);
        voucher.setQuantity(10);
        voucher.setMinOrderValue(BigDecimal.valueOf(5000000));

        when(voucherRepository.findByCode("SALE50")).thenReturn(Optional.of(voucher));
        when(voucherRepository.decrementQuantityIfAvailable(any())).thenReturn(1);
        when(voucherRepository.findById(any())).thenReturn(Optional.of(voucher));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order order = orderService.createOrderInstant(
                user, product, null, 1,
                "Le Van C", "789 Tran Hung Dao", "0912345678",
                PaymentMethod.COD, "SALE50"
        );

        assertNotNull(order);
        // Đơn 20 tr, 50% = 10 tr, nhưng bị khống chế tối đa 1 tr => Tổng tiền 19 tr
        assertEquals(BigDecimal.valueOf(19000000), order.getTotalAmount());
        assertEquals(BigDecimal.valueOf(1000000), order.getDiscount());
    }

    private Order buildOrder(OrderStatus status, boolean inventoryDeducted, int itemQty) {
        Order order = new Order();
        order.setId(10L);
        order.setOrderCode("DH20261118-0010");
        order.setUser(user);
        order.setReceiverName("Nguyen Van A");
        order.setReceiverPhone("0901234567");
        order.setShippingAddress("123 Le Loi");
        order.setOrderStatus(status);
        order.setInventoryDeducted(inventoryDeducted);
        order.setTotalAmount(BigDecimal.valueOf(20000000L * itemQty));

        OrderItem item = new OrderItem();
        item.setId(50L);
        item.setOrder(order);
        item.setProduct(product);
        item.setVariant(variant);
        item.setQuantity(itemQty);
        item.setPrice(BigDecimal.valueOf(20000000));
        order.setOrderItems(new ArrayList<>(List.of(item)));
        return order;
    }

    @Test
    @DisplayName("CHO_XAC_NHAN -> DA_XAC_NHAN: Trừ kho ngay lập tức, ghi lịch sử & bật inventoryDeducted")
    void testConfirmOrder_DeductsInventoryImmediately() {
        Order order = buildOrder(OrderStatus.CHO_XAC_NHAN, false, 2);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order updated = orderService.updateOrderStatus(10L, OrderStatus.DA_XAC_NHAN, "Admin", null);

        assertEquals(OrderStatus.DA_XAC_NHAN, updated.getOrderStatus());
        assertTrue(updated.isInventoryDeducted());
        assertEquals(3, variant.getStock()); // 5 - 2 = 3
        verify(productVariantRepository, atLeastOnce()).save(variant);
        verify(stockLogService, times(1)).logVariant(eq(product), eq(variant), eq(order), any(), eq(-2), eq(3), any(), any());
        verify(orderStatusHistoryRepository, times(1)).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("CHO_XAC_NHAN -> DA_XAC_NHAN thất bại nếu tồn kho không đủ (tránh overselling)")
    void testConfirmOrder_RejectsWhenInsufficientStock() {
        variant.setStock(1);
        Order order = buildOrder(OrderStatus.CHO_XAC_NHAN, false, 3);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.updateOrderStatus(10L, OrderStatus.DA_XAC_NHAN, "Admin", null));

        assertTrue(ex.getMessage().contains("Laptop Gaming ASUS ROG"));
        assertEquals(OrderStatus.CHO_XAC_NHAN, order.getOrderStatus());
        assertFalse(order.isInventoryDeducted());
        assertEquals(1, variant.getStock());
        verify(stockLogService, never()).logVariant(any(), any(), any(), any(), anyInt(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("Chống trừ kho nhiều lần qua các bước CHUAN_BI_HANG -> DANG_GIAO_HANG -> DA_GIAO_HANG -> HOAN_THANH")
    void testSubsequentTransitions_DoNotDeductInventoryAgain() {
        variant.setStock(3);
        Order order = buildOrder(OrderStatus.DA_XAC_NHAN, true, 2);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        orderService.updateOrderStatus(10L, OrderStatus.CHUAN_BI_HANG, "Admin", null);
        orderService.updateOrderStatus(10L, OrderStatus.DANG_GIAO_HANG, "Admin", null);
        orderService.updateOrderStatus(10L, OrderStatus.DA_GIAO_HANG, "Admin", null);
        orderService.updateOrderStatus(10L, OrderStatus.HOAN_THANH, "Admin", null);

        assertEquals(OrderStatus.HOAN_THANH, order.getOrderStatus());
        assertTrue(order.isInventoryDeducted());
        assertEquals(3, variant.getStock()); // Không bị trừ thêm lần nào
        verify(stockLogService, never()).logVariant(any(), any(), any(), any(), anyInt(), anyInt(), any(), any());
        verify(stockLogService, never()).log(any(), any(), any(), anyInt(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("Từ chối chuyển lùi hoặc nhảy bước trạng thái không hợp lệ")
    void testInvalidStateTransitions_Rejected() {
        Order order = buildOrder(OrderStatus.DA_XAC_NHAN, true, 1);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        // Nhảy cóc DA_XAC_NHAN -> HOAN_THANH
        assertThrows(IllegalStateException.class,
                () -> orderService.updateOrderStatus(10L, OrderStatus.HOAN_THANH, "Admin", null));

        // Chuyển lùi DA_XAC_NHAN -> CHO_XAC_NHAN
        assertThrows(IllegalStateException.class,
                () -> orderService.updateOrderStatus(10L, OrderStatus.CHO_XAC_NHAN, "Admin", null));

        // Cho phép đúng bước hợp lệ
        assertEquals(List.of(OrderStatus.CHUAN_BI_HANG, OrderStatus.DA_HUY),
                OrderStatus.DA_XAC_NHAN.getAllowedNextStatuses());
        assertFalse(orderService.isValidTransition(OrderStatus.DA_XAC_NHAN, OrderStatus.HOAN_THANH));
        assertTrue(orderService.isValidTransition(OrderStatus.DA_XAC_NHAN, OrderStatus.CHUAN_BI_HANG));
    }

    @Test
    @DisplayName("Hoàn kho tự động khi hủy đơn đã xác nhận / đang chuẩn bị và khi đã hoàn trả")
    void testRestoreInventory_OnCancelAfterConfirmOrReturnCompleted() {
        // 1. DA_XAC_NHAN -> DA_HUY (inventoryDeducted = true => hoàn kho + inventoryDeducted = false)
        variant.setStock(3);
        Order confirmedOrder = buildOrder(OrderStatus.DA_XAC_NHAN, true, 2);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(confirmedOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        orderService.updateOrderStatus(10L, OrderStatus.DA_HUY, "Admin", "Khách yêu cầu hủy");

        assertEquals(OrderStatus.DA_HUY, confirmedOrder.getOrderStatus());
        assertFalse(confirmedOrder.isInventoryDeducted());
        assertEquals(5, variant.getStock()); // 3 + 2 = 5
        verify(stockLogService, times(1)).logVariant(eq(product), eq(variant), eq(confirmedOrder), any(), eq(2), eq(5), any(), any());

        // 2. YEU_CAU_HOAN_TRA -> DA_HOAN_TRA (inventoryDeducted = true => hoàn kho 1 lần)
        variant.setStock(4);
        Order returningOrder = buildOrder(OrderStatus.YEU_CAU_HOAN_TRA, true, 1);
        returningOrder.setId(11L);
        when(orderRepository.findById(11L)).thenReturn(Optional.of(returningOrder));

        orderService.updateOrderStatus(11L, OrderStatus.DA_HOAN_TRA, "Admin", "Nhận lại hàng hoàn");

        assertEquals(OrderStatus.DA_HOAN_TRA, returningOrder.getOrderStatus());
        assertFalse(returningOrder.isInventoryDeducted());
        assertEquals(5, variant.getStock());

        // 3. CHO_XAC_NHAN -> DA_HUY (inventoryDeducted = false => không cộng khống tồn kho)
        variant.setStock(5);
        Order unconfirmedOrder = buildOrder(OrderStatus.CHO_XAC_NHAN, false, 2);
        unconfirmedOrder.setId(12L);
        when(orderRepository.findById(12L)).thenReturn(Optional.of(unconfirmedOrder));

        orderService.updateOrderStatus(12L, OrderStatus.DA_HUY, "Admin", null);
        assertEquals(OrderStatus.DA_HUY, unconfirmedOrder.getOrderStatus());
        assertFalse(unconfirmedOrder.isInventoryDeducted());
        assertEquals(5, variant.getStock());
    }

    @Test
    @DisplayName("Khóa dữ liệu đơn hàng khi ở trạng thái HOAN_THANH, nhưng vẫn cho phép YEU_CAU_HOAN_TRA")
    void testLockOrderEditing_WhenCompleted() {
        Order completedOrder = buildOrder(OrderStatus.HOAN_THANH, true, 1);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(completedOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        assertThrows(IllegalStateException.class,
                () -> orderService.updateAdminNote(10L, "Thử sửa ghi chú"));
        assertThrows(IllegalStateException.class,
                () -> orderService.updateOrderShippingInfo(10L, "B", "0999999999", "Dia chi moi"));
        assertThrows(IllegalStateException.class,
                () -> orderService.updateOrderItemQuantity(10L, 50L, 5));

        // Vẫn cho phép khởi tạo quy trình hoàn trả
        Order returnedReq = orderService.updateOrderStatus(10L, OrderStatus.YEU_CAU_HOAN_TRA, "Customer", "Lỗi bàn phím");
        assertEquals(OrderStatus.YEU_CAU_HOAN_TRA, returnedReq.getOrderStatus());
    }
}