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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private VoucherRepository voucherRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private StockLogService stockLogService;

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
    @DisplayName("Tạo đơn hàng từ giỏ hàng (COD) thành công - Trừ stock trực tiếp")
    void testCreateOrderFromCart_COD_Success() {
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
        assertEquals("Nguyen Van A", order.getReceiverName());
        assertEquals(BigDecimal.valueOf(40000000), order.getTotalAmount());
        assertEquals(3, variant.getStock()); // 5 - 2 = 3
        verify(productVariantRepository, times(1)).save(variant);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Tạo đơn hàng Mua Ngay với QR Code - Khóa tạm kho reservedStock")
    void testCreateOrderInstant_QR_ReservesStock() {
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order order = orderService.createOrderInstant(
                user, product, variant, 1,
                "Tran Van B", "456 Nguyen Hue", "0987654321",
                PaymentMethod.QR_CODE, null
        );

        assertNotNull(order);
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getOrderStatus());
        assertEquals(1, variant.getReservedStock()); // Khóa tạm 1
        assertEquals(5, variant.getStock()); // Tồn kho chính chưa trừ
        verify(productVariantRepository, times(1)).save(variant);
    }

    @Test
    @DisplayName("Áp dụng mã Voucher phần trăm có giới hạn giảm tối đa (Max Discount)")
    void testApplyVoucher_PercentageWithMaxLimit() {
        Voucher voucher = new Voucher();
        voucher.setCode("SALE50");
        voucher.setDiscountType("PERCENT");
        voucher.setDiscountValue(BigDecimal.valueOf(50)); // Giảm 50%
        voucher.setMaxDiscountAmount(BigDecimal.valueOf(1000000)); // Tối đa 1 triệu
        voucher.setActive(true);
        voucher.setQuantity(10);
        voucher.setMinOrderValue(BigDecimal.valueOf(5000000));

        when(voucherRepository.findByCode("SALE50")).thenReturn(Optional.of(voucher));
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
        assertEquals(9, voucher.getQuantity()); // Đã dùng 1 lần
    }
}