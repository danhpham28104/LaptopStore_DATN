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

    @Autowired
    private com.laptopstore.laptopstore.Service.ReviewService reviewService;

    @Override
    public void run(String... args) throws Exception {
        log.info("[DataInitializer] Kiểm tra và mở khoá tài khoản hệ thống...");

        // 0. Migration các chuỗi order_status cũ trong DB sang tên Enum chuẩn mới
        try {
            jdbcTemplate.update("UPDATE orders SET order_status = 'CHO_THANH_TOAN' WHERE order_status IN ('PENDING_PAYMENT', 'Pending Payment')");
            jdbcTemplate.update("UPDATE orders SET order_status = 'CHO_XAC_NHAN' WHERE order_status IN ('PENDING', 'Pending', 'pending')");
            jdbcTemplate.update("UPDATE orders SET order_status = 'DA_XAC_NHAN' WHERE order_status IN ('CONFIRMED', 'Confirmed', 'confirmed', 'PAID', 'Paid', 'paid')");
            jdbcTemplate.update("UPDATE orders SET order_status = 'CHUAN_BI_HANG' WHERE order_status IN ('PROCESSING', 'PACKING', 'Processing', 'Packing')");
            jdbcTemplate.update("UPDATE orders SET order_status = 'DANG_GIAO_HANG' WHERE order_status IN ('SHIPPING', 'SHIPPED', 'Shipped', 'shipped')");
            jdbcTemplate.update("UPDATE orders SET order_status = 'DA_GIAO_HANG' WHERE order_status IN ('DELIVERED', 'Delivered', 'delivered')");
            jdbcTemplate.update("UPDATE orders SET order_status = 'HOAN_THANH' WHERE order_status IN ('COMPLETED', 'Completed', 'completed')");
            jdbcTemplate.update("UPDATE orders SET order_status = 'DA_HUY' WHERE order_status IN ('CANCELLED', 'Cancelled', 'cancelled', 'Payment Timeout', 'Payment Failed', 'PAYMENT_TIMEOUT', 'PAYMENT_FAILED')");
            jdbcTemplate.update("UPDATE orders SET order_status = 'YEU_CAU_HOAN_TRA' WHERE order_status IN ('RETURN_REQUESTED')");
            jdbcTemplate.update("UPDATE orders SET order_status = 'DA_HOAN_TRA' WHERE order_status IN ('RETURNED')");
            jdbcTemplate.update("UPDATE orders SET order_status = 'DA_HOAN_TIEN' WHERE order_status IN ('REFUNDED')");

            // Đồng bộ cờ inventory_deducted cho các đơn hàng cũ đã ở trạng thái đã giữ/trừ kho
            jdbcTemplate.update("UPDATE orders SET inventory_deducted = true WHERE inventory_deducted = false AND order_status IN ('DA_XAC_NHAN', 'CHUAN_BI_HANG', 'DANG_GIAO_HANG', 'DA_GIAO_HANG', 'HOAN_THANH', 'YEU_CAU_HOAN_TRA')");

            // Đồng bộ lịch sử trạng thái cũ
            jdbcTemplate.update("UPDATE order_status_history SET new_status = 'CHO_THANH_TOAN' WHERE new_status = 'PENDING_PAYMENT'");
            jdbcTemplate.update("UPDATE order_status_history SET new_status = 'CHO_XAC_NHAN' WHERE new_status = 'PENDING'");
            jdbcTemplate.update("UPDATE order_status_history SET new_status = 'DA_XAC_NHAN' WHERE new_status IN ('CONFIRMED', 'PAID')");
            jdbcTemplate.update("UPDATE order_status_history SET new_status = 'CHUAN_BI_HANG' WHERE new_status IN ('PROCESSING', 'PACKING')");
            jdbcTemplate.update("UPDATE order_status_history SET new_status = 'DANG_GIAO_HANG' WHERE new_status IN ('SHIPPING', 'SHIPPED')");
            jdbcTemplate.update("UPDATE order_status_history SET new_status = 'DA_GIAO_HANG' WHERE new_status = 'DELIVERED'");
            jdbcTemplate.update("UPDATE order_status_history SET new_status = 'HOAN_THANH' WHERE new_status = 'COMPLETED'");
            jdbcTemplate.update("UPDATE order_status_history SET new_status = 'DA_HUY' WHERE new_status IN ('CANCELLED', 'PAYMENT_TIMEOUT', 'PAYMENT_FAILED')");
            jdbcTemplate.update("UPDATE order_status_history SET new_status = 'YEU_CAU_HOAN_TRA' WHERE new_status = 'RETURN_REQUESTED'");
            jdbcTemplate.update("UPDATE order_status_history SET new_status = 'DA_HOAN_TRA' WHERE new_status = 'RETURNED'");
            jdbcTemplate.update("UPDATE order_status_history SET new_status = 'DA_HOAN_TIEN' WHERE new_status = 'REFUNDED'");
        } catch (Exception e) {
            log.warn("[DataInitializer] SQL Migration order_status: {}", e.getMessage());
        }

        // 0b. Xóa ràng buộc unique cũ trên cột model (cho phép nhiều máy trùng model nhưng khác mã ID xuất xưởng)
        try {
            jdbcTemplate.execute("ALTER TABLE product DROP INDEX uk_product_model");
        } catch (Exception ignored) {}

        // 0c. Tự động điền product_code cho các sản phẩm cũ chưa có mã ID xuất xưởng
        try {
            jdbcTemplate.update("UPDATE product SET product_code = CONCAT('LP-', LPAD(id, 4, '0')) WHERE product_code IS NULL OR product_code = ''");
        } catch (Exception e) {
            log.warn("[DataInitializer] SQL Migration product_code: {}", e.getMessage());
        }

        // 0d. Migration bảng reviews: đồng bộ updated_at, gộp review trùng lặp (giữ bản mới nhất), recalculate rating & thêm unique constraint
        try {
            jdbcTemplate.update("UPDATE reviews SET updated_at = created_at WHERE updated_at IS NULL");
        } catch (Exception e) {
            log.warn("[DataInitializer] SQL Migration reviews.updated_at: {}", e.getMessage());
        }
        try {
            reviewService.cleanupDuplicateReviewsAndRecalculate();
        } catch (Exception e) {
            log.warn("[DataInitializer] Migration cleanupDuplicateReviewsAndRecalculate: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute("ALTER TABLE reviews ADD CONSTRAINT uk_review_user_product_order_item UNIQUE (user_id, product_id, order_item_id)");
        } catch (Exception ignored) {
            // Constraint đã tồn tại trước đó
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
