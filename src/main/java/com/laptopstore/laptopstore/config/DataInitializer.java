package com.laptopstore.laptopstore.config;

import com.laptopstore.laptopstore.Repository.RoleRepository;
import com.laptopstore.laptopstore.Repository.UserRepository;
import com.laptopstore.laptopstore.entity.Role;
import com.laptopstore.laptopstore.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tự động chạy khi ứng dụng khởi động:
 * 1. Đảm bảo toàn bộ tài khoản ADMIN luôn ở trạng thái enabled = true (Mở khoá tuyệt đối).
 * 2. Tự động khắc phục dữ liệu cũ nếu bị gán enabled = false do thêm cột DB.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.laptopstore.laptopstore.Repository.CategoryRepository categoryRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("[DataInitializer] Kiểm tra và mở khoá tài khoản hệ thống...");

        // 0. Migration các chuỗi order_status cũ trong DB sang tên Enum chuẩn
        try {
            jdbcTemplate.update("UPDATE orders SET order_status = 'PENDING_PAYMENT' WHERE order_status IN ('Pending', 'pending')");
            jdbcTemplate.update("UPDATE orders SET order_status = 'CONFIRMED' WHERE order_status IN ('Paid', 'paid', 'Confirmed', 'confirmed')");
            jdbcTemplate.update("UPDATE orders SET order_status = 'SHIPPING' WHERE order_status IN ('Shipped', 'shipped')");
            jdbcTemplate.update("UPDATE orders SET order_status = 'DELIVERED' WHERE order_status IN ('Delivered', 'delivered', 'Completed', 'completed')");
            jdbcTemplate.update("UPDATE orders SET order_status = 'CANCELLED' WHERE order_status IN ('Cancelled', 'cancelled', 'Payment Timeout', 'Payment Failed')");
        } catch (Exception e) {
            log.warn("[DataInitializer] SQL Migration order_status: {}", e.getMessage());
        }

        // 1. Đảm bảo các Role cơ bản tồn tại
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
            .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_ADMIN")));
        roleRepository.findByName("ROLE_SALE")
            .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_SALE")));
        roleRepository.findByName("ROLE_WAREHOUSE")
            .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_WAREHOUSE")));
        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_USER")));

        // Khắc phục / Migration dữ liệu cũ nếu DB có Role tên "USER" không có prefix "ROLE_"
        roleRepository.findByName("USER").ifPresent(legacyUserRole -> {
            log.info("[DataInitializer] Phát hiện Role legacy 'USER' - Đang chuyển đổi sang 'ROLE_USER'...");
            if (!legacyUserRole.getId().equals(userRole.getId())) {
                List<User> usersWithLegacyRole = userRepository.findAll().stream()
                        .filter(u -> u.getRoles().contains(legacyUserRole))
                        .toList();
                for (User u : usersWithLegacyRole) {
                    u.getRoles().remove(legacyUserRole);
                    u.getRoles().add(userRole);
                    userRepository.save(u);
                }
                try {
                    roleRepository.delete(legacyUserRole);
                } catch (Exception e) {
                    log.warn("[DataInitializer] Không thể xóa role legacy 'USER': {}", e.getMessage());
                }
            } else {
                legacyUserRole.setName("ROLE_USER");
                roleRepository.save(legacyUserRole);
            }
            log.info("[DataInitializer] ✅ Đã hoàn tất chuyển đổi Role legacy 'USER' -> 'ROLE_USER'.");
        });

        // 2. Lấy toàn bộ người dùng trong DB
        List<User> allUsers = userRepository.findAll();

        int unlockedAdmins = 0;
        int restoredUsers = 0;

        for (User u : allUsers) {
            boolean isAdmin = u.getRoles().stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));

            // Tài khoản ADMIN -> LUÔN MỞ KHÓA (enabled = true)
            if (isAdmin) {
                if (!u.isEnabled()) {
                    u.setEnabled(true);
                    userRepository.save(u);
                    unlockedAdmins++;
                }
            } else {
                // Nếu dữ liệu cũ bị ddl-auto gán false ngoài ý muốn (không có lệnh khoá từ admin), ta bật lại
                if (!u.isEnabled()) {
                    u.setEnabled(true);
                    userRepository.save(u);
                    restoredUsers++;
                }
            }
        }

        if (unlockedAdmins > 0) {
            log.info("[DataInitializer] ✅ Đã mở khoá {} tài khoản ADMIN!", unlockedAdmins);
        }
        if (restoredUsers > 0) {
            log.info("[DataInitializer] ✅ Đã khôi phục trạng thái hoạt động cho {} người dùng!", restoredUsers);
        }

        // 3. Đảm bảo các Category mặc định tồn tại
        if (categoryRepository.count() == 0) {
            log.info("[DataInitializer] Khởi tạo các danh mục mặc định...");
            createCategoryIfNotExist("Laptop Gaming", "laptop-gaming", "bi-controller", "Cấu hình khủng, card đồ họa rời cho game thủ");
            createCategoryIfNotExist("Laptop Văn phòng", "laptop-van-phong", "bi-briefcase", "Mỏng nhẹ, pin trâu, mượt mà các tác vụ văn phòng");
            createCategoryIfNotExist("Laptop Đồ họa", "laptop-do-hoa", "bi-palette", "Màn hình chuẩn màu, hiệu năng cao cho thiết kế & đồ họa");
            createCategoryIfNotExist("Laptop Sinh viên", "laptop-sinh-vien", "bi-mortarboard", "Giá cả hợp lý, đáp ứng nhu cầu học tập và giải trí");
            log.info("[DataInitializer] ✅ Đã khởi tạo thành công 4 danh mục mặc định!");
        }

        log.info("[DataInitializer] Kiểm tra hoàn tất. Tất cả tài khoản ADMIN đều đang hoạt động.");
    }

    private void createCategoryIfNotExist(String name, String slug, String icon, String description) {
        if (!categoryRepository.existsByName(name)) {
            com.laptopstore.laptopstore.entity.Category cat = new com.laptopstore.laptopstore.entity.Category();
            cat.setName(name);
            cat.setSlug(slug);
            cat.setIcon(icon);
            cat.setDescription(description);
            cat.setActive(true);
            categoryRepository.save(cat);
        }
    }
}
