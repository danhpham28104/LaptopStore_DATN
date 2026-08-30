package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.CartRepository;
import com.laptopstore.laptopstore.Repository.RoleRepository;
import com.laptopstore.laptopstore.Repository.UserRepository;
import com.laptopstore.laptopstore.entity.Cart;
import com.laptopstore.laptopstore.entity.Role;
import com.laptopstore.laptopstore.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CartRepository cartRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User newUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("newuser@example.com");
        newUser.setPassword("password123");

        userRole = new Role(1L, "ROLE_USER");
    }

    @Test
    @DisplayName("Đăng ký tài khoản mới thành công - Gán đúng ROLE_USER với prefix ROLE_")
    void testRegister_AssignsRoleUserWithPrefix() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registered = authService.register(newUser);

        assertNotNull(registered);
        assertEquals(1, registered.getRoles().size());
        assertTrue(registered.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_USER")),
                "User mới đăng ký phải có role 'ROLE_USER'");
        verify(roleRepository, times(1)).findByName("ROLE_USER");
    }
}
