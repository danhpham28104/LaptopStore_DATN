package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.RoleRepository;
import com.laptopstore.laptopstore.Repository.UserRepository;
import com.laptopstore.laptopstore.entity.Role;
import com.laptopstore.laptopstore.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    public boolean checkPassword(User user, String rawPassword) {

        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUser(){
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id){
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username){
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    @Transactional
    public Optional<User> findByUsername(String loginName) {
        return userRepository.findByUsername(loginName);
    }
    public long countUsers() {
        return userRepository.count();
    }

    /**
     * 🔹 Tạo tài khoản nội bộ (Admin/Sale/Warehouse/User)
     */
    @Transactional
    public User createUserInternal(String fullName, String username, String email, String phone, String rawPassword, String roleName) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Tên đăng nhập '" + username + "' đã tồn tại!");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email '" + email + "' đã tồn tại!");
        }

        User u = new User();
        u.setFullName(fullName);
        u.setUsername(username);
        u.setEmail(email);
        u.setPhone(phone);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setEnabled(true);

        // Gán Role
        String formattedRole = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        Role role = roleRepository.findByName(formattedRole)
            .orElseGet(() -> roleRepository.save(new Role(null, formattedRole)));

        u.getRoles().add(role);
        return userRepository.save(u);
    }

    /**
     * 🔹 Đổi role cho user (Phương án A: 1 User có 1 Role duy nhất)
     */
    @Transactional
    public void changeUserRole(Long targetUserId, String newRoleName, String currentUsername) {
        User targetUser = userRepository.findById(targetUserId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Ràng buộc 1: Không cho tự đổi role của chính mình
        if (targetUser.getUsername().equalsIgnoreCase(currentUsername)) {
            throw new RuntimeException("Bạn không thể tự đổi vai trò của chính tài khoản mình đang đăng nhập!");
        }

        // Ràng buộc 2: Nếu targetUser đang là ADMIN, kiểm tra xem còn ít nhất 1 ADMIN khác không
        boolean targetIsAdmin = targetUser.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));
        String formattedRole = newRoleName.startsWith("ROLE_") ? newRoleName : "ROLE_" + newRoleName;

        if (targetIsAdmin && !"ROLE_ADMIN".equalsIgnoreCase(formattedRole)) {
            long adminCount = userRepository.countByRoles_Name("ROLE_ADMIN");
            if (adminCount <= 1) {
                throw new RuntimeException("Hệ thống phải có ít nhất 1 tài khoản ADMIN. Không thể hạ cấp tài khoản này!");
            }
        }

        // Đổi role: Xóa toàn bộ role cũ, gán role mới
        Role newRole = roleRepository.findByName(formattedRole)
            .orElseGet(() -> roleRepository.save(new Role(null, formattedRole)));

        targetUser.getRoles().clear();
        targetUser.getRoles().add(newRole);
        userRepository.save(targetUser);
    }

    /**
     * 🔹 Bật / Tắt trạng thái khóa tài khoản
     * ⚠️ RÀNG BUỘC: Tài khoản ADMIN tuyệt đối KHÔNG THỂ bị khoá trong bất kỳ trường hợp nào!
     */
    @Transactional
    public boolean toggleUserStatus(Long targetUserId, String currentUsername) {
        User targetUser = userRepository.findById(targetUserId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Ràng buộc 1: Không cho tự khóa tài khoản của chính mình
        if (targetUser.getUsername().equalsIgnoreCase(currentUsername)) {
            throw new RuntimeException("Bạn không thể tự khoá tài khoản của chính mình!");
        }

        // Ràng buộc 2: TÀI KHOẢN ADMIN LUÔN LUÔN MỞ, TUYỆT ĐỐI KHÔNG THỂ BỊ KHÓA
        boolean targetIsAdmin = targetUser.getRoles().stream()
            .anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));

        if (targetIsAdmin) {
            // Đảm bảo admin luôn enabled = true
            if (!targetUser.isEnabled()) {
                targetUser.setEnabled(true);
                userRepository.save(targetUser);
            }
            throw new RuntimeException("Tài khoản ADMIN là quyền quản trị tối cao, LUÔN MỞ và không thể bị khoá!");
        }

        targetUser.setEnabled(!targetUser.isEnabled());
        userRepository.save(targetUser);
        return targetUser.isEnabled();
    }
}
