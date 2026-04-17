package com.techstore.techstore.Controller;

import com.techstore.techstore.Service.AuthService;
import com.techstore.techstore.Service.UserService;
import com.techstore.techstore.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Controller cho đăng nhập & đăng ký (Thymeleaf)
 */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot_password";
    }

    @PostMapping("/forgot-password")
    public String sendCode(@RequestParam String email, Model model) {
        boolean ok = authService.sendResetCode(email);

        if (!ok) {
            model.addAttribute("error", "Email không tồn tại!");
            return "auth/forgot_password";
        }

        model.addAttribute("email", email);
        return "auth/verify_code";
    }

    @PostMapping("/verify-code")
    public String verifyCode(@RequestParam String email,
                             @RequestParam String code,
                             Model model) {

        Optional<User> user = userService.findByEmail(email);
        if (user.isEmpty()) {
            model.addAttribute("error", "Email không tồn tại!");
            return "auth/forgot_password";
        }

        if (!code.equals(user.get().getResetCode())) {
            model.addAttribute("error", "Mã không đúng!");
            model.addAttribute("email", email);
            return "auth/verify_code";
        }
        if (user.get().getResetCodeExpire().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "Mã đã hết hạn!");
            model.addAttribute("email", email);
            return "auth/verify_code";
        }


        model.addAttribute("email", email);
        model.addAttribute("code", code);
        return "auth/reset_password";
    }


    @PostMapping("/reset-password")
    public String resetPass(@RequestParam String email,
                            @RequestParam String code,
                            @RequestParam String password,
                            Model model) {

        boolean ok = authService.resetPassword(email, code, password);

        if (!ok) {
            model.addAttribute("email", email);
            model.addAttribute("code", code);
            model.addAttribute("error", "Mã khôi phục không hợp lệ hoặc đã hết hạn!");
            return "auth/reset_password";
        }


        return "redirect:/login?reset_success";
    }

    /** Trang đăng nhập */
    @GetMapping("/login")
    public String showLoginPage() {
        return "login"; // ↔ templates/login.html
    }


    /** Trang đăng ký */
    @GetMapping("/register")
    public String showSignupPage() {
        return "register"; // ↔ templates/register.html
    }

    @PostMapping("/auth/register")
    public String signup(@RequestParam String username,
                         @RequestParam String email,
                         @RequestParam String password,
                         @RequestParam String fullName,
                         RedirectAttributes redirectAttributes,
                         Model model) {

        try {
            User u = new User();
            u.setUsername(username);
            u.setEmail(email);
            u.setPassword(password);
            u.setFullName(fullName);
            u.setCreatedAt(LocalDateTime.now());
            u.setDeleted(false);

            authService.register(u);

            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Hãy đăng nhập.");
            return "redirect:/login";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("message", ex.getMessage());
            return "register";
        }
    }

}
