package com.laptopstore.laptopstore.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

/**
 * Xử lý redirect sau khi đăng nhập thành công dựa vào Role.
 * - ROLE_ADMIN       → /admin (Dashboard tổng quan)
 * - ROLE_SALE        → /admin/orders (Quản lý đơn hàng)
 * - ROLE_WAREHOUSE   → /admin/products (Quản lý sản phẩm)
 * - ROLE_USER        → /home
 */
@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        String redirectUrl = "/home"; // default

        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equals(role)) {
                redirectUrl = "/admin";
                break;
            } else if ("ROLE_SALE".equals(role)) {
                redirectUrl = "/admin/orders";
                break;
            } else if ("ROLE_WAREHOUSE".equals(role)) {
                redirectUrl = "/admin/products";
                break;
            }
        }

        response.sendRedirect(redirectUrl);
    }
}
