package com.techstore.techstore.Service;

import com.techstore.techstore.Repository.CartRepository;
import com.techstore.techstore.Repository.RoleRepository;
import com.techstore.techstore.Repository.UserRepository;
import com.techstore.techstore.entity.Cart;
import com.techstore.techstore.entity.Role;
import com.techstore.techstore.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CartRepository cartRepository;
    private final EmailService emailService;


    /** Gửi mã reset */
    public boolean sendResetCode(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();

        // Tạo mã 6 số
        String code = String.format("%06d", new Random().nextInt(999999));
        user.setResetCode(code);
        user.setResetCodeExpire(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        // Gửi email qua SendGrid
        try {
            emailService.sendEmail(
                    email,
                    "TechStore - Mã khôi phục mật khẩu",
                    "Mã đặt lại mật khẩu của bạn là: " + code + "\nCó hiệu lực trong 10 phút."
            );
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /** Kiểm tra mã + đặt lại mật khẩu */
    public boolean resetPassword(String email, String code, String newPassword) {

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();

        // Kiểm tra mã hết hạn
        if (user.getResetCodeExpire() == null ||
                user.getResetCodeExpire().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Kiểm tra mã đúng
        if (!code.equals(user.getResetCode())) {
            return false;
        }

        // Update mật khẩu (bạn nhớ thêm password encoder nhé)
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetCode(null);
        user.setResetCodeExpire(null);

        userRepository.save(user);
        return true;
    }


    @Transactional
    public User register(User user) {
        // --- Kiểm tra trùng username / email ---
        if (userRepository.existsByUsername(user.getUsername()))
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        if (userRepository.existsByEmail(user.getEmail()))
            throw new IllegalArgumentException("Email đã tồn tại");

        // --- Mã hoá mật khẩu ---
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // --- Gán role mặc định ---
        Role roleDefault = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName("USER");
                    return roleRepository.save(newRole);
                });
        user.setRoles(Set.of(roleDefault));

        // --- Lưu user trước để lấy ID ---
        User savedUser = userRepository.save(user);

        // --- Tạo giỏ hàng trống cho user ---
        Cart cart = new Cart();
        cart.setUser(savedUser);
        cart.setItems(new java.util.ArrayList<>());
        cart.setSubtotal(java.math.BigDecimal.ZERO);
        cart.setDiscount(java.math.BigDecimal.ZERO);
        cart.setTotal(java.math.BigDecimal.ZERO);

        cartRepository.save(cart);

        return savedUser;
    }
}
