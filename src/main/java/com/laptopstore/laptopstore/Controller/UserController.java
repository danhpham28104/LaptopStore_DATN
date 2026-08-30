package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.AddressService;
import com.laptopstore.laptopstore.Service.UserService;
import com.laptopstore.laptopstore.entity.Address;
import com.laptopstore.laptopstore.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private AddressService addressService;

    /** Trang danh sách user (admin) -> Chuyển hướng sang /admin/users */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String listUsers() {
        return "redirect:/admin/users";
    }

    /** Trang tài khoản hiện tại */
    @GetMapping("/user/account")
    public String currentUserAccount(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        // 🔹 Spring Security mặc định principal.getName() = username
        String username = principal.getName();
        User user = userService.findByUsername(username).orElse(null);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("addresses", user.getAddresses());
        model.addAttribute("newAddress", new Address());
        return "account"; // ↔ templates/account.html
    }

    @PostMapping("/user/update")
    public String updateUser(@RequestParam Long id,
                             @RequestParam String fullName,
                             @RequestParam String email,
                             @RequestParam String phone,
                             Model model,
                             Principal principal) {
        if (principal == null) return "redirect:/login";
        User loggedInUser = userService.findByUsername(principal.getName()).orElse(null);
        if (loggedInUser == null) return "redirect:/login";

        // 🛡️ BẢO MẬT: Chỉ cho phép cập nhật thông tin cá nhân của chính mình
        if (!loggedInUser.getId().equals(id)) {
            model.addAttribute("error", "Bạn không có quyền chỉnh sửa thông tin tài khoản này!");
            return currentUserAccount(model, principal);
        }

        User u = userService.getUserById(id).orElseThrow();
        u.setFullName(fullName);
        u.setEmail(email);
        u.setPhone(phone);
        u.setUpdatedAt(LocalDateTime.now());
        userService.saveUser(u);

        model.addAttribute("success", "Cập nhật thông tin thành công!");
        return currentUserAccount(model, principal);
    }


    /** Thêm địa chỉ mới */
    @PostMapping("/user/account/add-address")
    public String addAddress(@ModelAttribute("newAddress") Address address, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        addressService.addAddress(user.getId(), address);
        return "redirect:/user/account#addresses";
    }


    @PostMapping("/user/account/change-password")
    public String changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Principal principal,
            Model model
    ) {
        if (principal == null) return "redirect:/login";

        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        // Kiểm tra xác nhận
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp.");
            return currentUserAccount(model, principal);
        }

        // Kiểm tra mật khẩu cũ đúng không
        if (!userService.checkPassword(user, oldPassword)) {
            model.addAttribute("error", "Mật khẩu hiện tại không chính xác.");
            return currentUserAccount(model, principal);
        }

        // Cập nhật mật khẩu
        userService.updatePassword(user, newPassword);
        model.addAttribute("success", "Đổi mật khẩu thành công!");
        return currentUserAccount(model, principal);
    }


    /** Xóa địa chỉ */
    @GetMapping("/user/account/delete-address/{id}")
    public String deleteAddress(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        // 🛡️ BẢO MẬT: Kiểm tra quyền sở hữu địa chỉ trước khi xóa
        Address address = addressService.getById(id);
        if (address != null && address.getUser() != null && address.getUser().getId().equals(user.getId())) {
            addressService.deleteAddress(id);
        }
        return "redirect:/user/account#addresses";
    }

    /** Đặt địa chỉ mặc định */
    @GetMapping("/account/make-default/{id}")
    public String makeDefault(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        // 🛡️ BẢO MẬT: Kiểm tra quyền sở hữu địa chỉ trước khi đặt làm mặc định
        Address address = addressService.getById(id);
        if (address != null && address.getUser() != null && address.getUser().getId().equals(user.getId())) {
            addressService.setDefaultAddress(user.getId(), id);
        }
        return "redirect:/user/account#addresses";
    }
}
