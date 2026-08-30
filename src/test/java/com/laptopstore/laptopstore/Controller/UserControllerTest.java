package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.AddressService;
import com.laptopstore.laptopstore.Service.UserService;
import com.laptopstore.laptopstore.entity.Address;
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
class UserControllerTest {

    @Mock private UserService userService;
    @Mock private AddressService addressService;
    @Mock private Model model;
    @Mock private Principal principal;

    @InjectMocks
    private UserController userController;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = new User();
        userA.setId(1L);
        userA.setUsername("userA");

        userB = new User();
        userB.setId(2L);
        userB.setUsername("userB");
    }

    @Test
    @DisplayName("Gửi request /users -> Redirect sang /admin/users")
    void testListUsers_RedirectsToAdminUsers() {
        String view = userController.listUsers();
        assertEquals("redirect:/admin/users", view);
    }

    @Test
    @DisplayName("Cập nhật tài khoản của người khác - Chặn và hiển thị thông báo lỗi")
    void testUpdateUser_Unauthorized_ReturnsError() {
        when(principal.getName()).thenReturn("userA");
        when(userService.findByUsername("userA")).thenReturn(Optional.of(userA));

        // Attempting to update userB's id (2L) while logged in as userA (1L)
        String view = userController.updateUser(2L, "Fake Name", "fake@test.com", "0900000000", model, principal);

        verify(userService, never()).saveUser(any());
        verify(model).addAttribute(eq("error"), contains("không có quyền"));
    }

    @Test
    @DisplayName("Xóa địa chỉ của người khác - Chặn và KHÔNG gọi addressService.deleteAddress")
    void testDeleteAddress_Unauthorized_DoesNotDelete() {
        Address addressOfUserB = new Address();
        addressOfUserB.setId(50L);
        addressOfUserB.setUser(userB);

        when(principal.getName()).thenReturn("userA");
        when(userService.findByUsername("userA")).thenReturn(Optional.of(userA));
        when(addressService.getById(50L)).thenReturn(addressOfUserB);

        String view = userController.deleteAddress(50L, principal);

        assertEquals("redirect:/user/account#addresses", view);
        verify(addressService, never()).deleteAddress(50L);
    }
}
