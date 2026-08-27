package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Repository.RoleRepository;
import com.laptopstore.laptopstore.Service.UserService;
import com.laptopstore.laptopstore.entity.Role;
import com.laptopstore.laptopstore.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller quản lý tài khoản người dùng & phân quyền.
 * Chỉ dành cho ROLE_ADMIN.
 */
@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    /** 🔹 Danh sách người dùng */
    @GetMapping
    public String listUser(Model model) {
        List<User> users = userService.getAllUser();
        List<Role> roles = roleRepository.findAll();

        model.addAttribute("users", users);
        model.addAttribute("roles", roles);
        model.addAttribute("pageTitle", "Quản Lý Tài Khoản & Phân Quyền - LaptopStore Admin");
        model.addAttribute("active", "users");
        return "admin/users";
    }

    /** 🔹 Tạo tài khoản nội bộ mới (Admin, Sale, Warehouse, User) */
    @PostMapping("/add")
    public String addUser(
            @RequestParam String fullName,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            @RequestParam String password,
            @RequestParam String roleName,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.createUserInternal(fullName, username, email, phone, password, roleName);
            redirectAttributes.addAttribute("success", "added");
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /** 🔹 Cập nhật thông tin & Role cho tài khoản */
    @PostMapping("/edit/{id}")
    public String editUser(
            @PathVariable Long id,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String roleName,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User user = userService.getUserById(id).orElse(null);
            if (user == null) {
                redirectAttributes.addAttribute("error", "Không tìm thấy người dùng");
                return "redirect:/admin/users";
            }

            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhone(phone);
            userService.saveUser(user);

            // Đổi role nếu được chỉ định
            if (roleName != null && !roleName.isBlank()) {
                userService.changeUserRole(id, roleName, authentication.getName());
            }

            redirectAttributes.addAttribute("success", "updated");
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /** 🔹 Khóa / Mở khóa tài khoản */
    @PostMapping("/toggle-status/{id}")
    public String toggleStatus(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            boolean isEnabled = userService.toggleUserStatus(id, authentication.getName());
            redirectAttributes.addAttribute("success", isEnabled ? "unlocked" : "locked");
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /** 🔹 Xóa tài khoản */
    @PostMapping("/delete/{id}")
    public String deleteUser(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User user = userService.getUserById(id).orElse(null);
            if (user != null && user.getUsername().equalsIgnoreCase(authentication.getName())) {
                redirectAttributes.addAttribute("error", "Bạn không thể xóa tài khoản của chính mình!");
                return "redirect:/admin/users";
            }
            userService.deleteUser(id);
            redirectAttributes.addAttribute("success", "deleted");
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
