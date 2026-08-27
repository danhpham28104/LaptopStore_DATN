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

    @Override
    public void run(String... args) throws Exception {
        log.info("[DataInitializer] Kiểm tra và mở khoá tài khoản hệ thống...");

        // 1. Đảm bảo các Role cơ bản tồn tại
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
            .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_ADMIN")));
        roleRepository.findByName("ROLE_SALE")
            .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_SALE")));
        roleRepository.findByName("ROLE_WAREHOUSE")
            .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_WAREHOUSE")));
        roleRepository.findByName("ROLE_USER")
            .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_USER")));

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

        log.info("[DataInitializer] Kiểm tra hoàn tất. Tất cả tài khoản ADMIN đều đang hoạt động.");
    }
}
