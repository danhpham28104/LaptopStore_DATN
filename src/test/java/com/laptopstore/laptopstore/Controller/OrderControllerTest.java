package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.OrderService;
import com.laptopstore.laptopstore.Service.UserService;
import com.laptopstore.laptopstore.entity.Order;
import com.laptopstore.laptopstore.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock private OrderService orderService;
    @Mock private UserService userService;
    @Mock private Model model;
    @Mock private Principal principal;

    @InjectMocks
    private OrderController orderController;

    private User userA;
    private User userB;
    private Order orderB;

    @BeforeEach
    void setUp() {
        userA = new User();
        userA.setId(1L);
        userA.setUsername("userA");

        userB = new User();
        userB.setId(2L);
        userB.setUsername("userB");

        orderB = new Order();
        orderB.setId(100L);
        orderB.setUser(userB);
    }

    @Test
    @DisplayName("Xem chi tiết đơn hàng của người khác - Chặn và Redirect với error=unauthorized")
    void testOrderDetail_UnauthorizedUser_RedirectsWithError() {
        when(principal.getName()).thenReturn("userA");
        when(orderService.getOrderById(100L)).thenReturn(Optional.of(orderB));

        String viewName = orderController.orderDetail(100L, principal, model);

        assertEquals("redirect:/orders?error=unauthorized", viewName);
        verify(model, never()).addAttribute(eq("order"), any());
    }

    @Test
    @DisplayName("Hủy đơn hàng của người khác - Chặn và KHÔNG gọi cancelOrder trong service")
    void testCancelOrder_UnauthorizedUser_PreventsCancellation() {
        when(principal.getName()).thenReturn("userA");
        when(orderService.getOrderById(100L)).thenReturn(Optional.of(orderB));

        String viewName = orderController.cancelOrder(100L, principal);

        assertEquals("redirect:/orders?error=unauthorized", viewName);
        verify(orderService, never()).cancelOrder(100L);
    }

    @Test
    @DisplayName("Xem chi tiết đơn hàng của chính mình - Cho phép truy cập")
    void testOrderDetail_AuthorizedOwner_Success() {
        when(principal.getName()).thenReturn("userB");
        when(orderService.getOrderById(100L)).thenReturn(Optional.of(orderB));

        String viewName = orderController.orderDetail(100L, principal, model);

        assertEquals("order_detail", viewName);
        verify(model).addAttribute("order", orderB);
    }
}
