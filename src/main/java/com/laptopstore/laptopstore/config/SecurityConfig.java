package com.laptopstore.laptopstore.config;

import com.laptopstore.laptopstore.security.CustomUserDetailsService;
import com.laptopstore.laptopstore.security.CustomSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình bảo mật với phân quyền đa tầng (RBAC):
 *
 * ROLE_ADMIN     → Toàn quyền admin
 * ROLE_SALE      → Chỉ xem/xử lý Đơn hàng, Voucher, Đánh giá
 * ROLE_WAREHOUSE → Chỉ xem/thêm/sửa Sản phẩm, Thương hiệu, Kho
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomSuccessHandler successHandler;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          CustomSuccessHandler successHandler) {
        this.userDetailsService = userDetailsService;
        this.successHandler = successHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/otp/**", "/api/**", "/webhook/**", "/payment/webhook/**", "/v3/api-docs/**", "/swagger-ui/**")
            )
            .authorizeHttpRequests(auth -> auth
                // ─── SWAGGER OPENAPI (Public) ───
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // ─── ADMIN ENTRY (Cho phép ADMIN, SALE, WAREHOUSE truy cập /admin) ───
                .requestMatchers("/admin", "/admin/").hasAnyRole("ADMIN", "SALE", "WAREHOUSE")
                .requestMatchers("/admin/dashboard/**").hasRole("ADMIN")
                .requestMatchers("/admin/users/**").hasRole("ADMIN")
                .requestMatchers("/admin/export/**").hasRole("ADMIN")
                .requestMatchers("/admin/analytics/**").hasRole("ADMIN")
                .requestMatchers("/users", "/users/**").hasRole("ADMIN")

                // ─── ADMIN + SALE (Đơn hàng, Voucher, Review) ───
                .requestMatchers("/admin/orders/**").hasAnyRole("ADMIN", "SALE")
                .requestMatchers("/admin/vouchers/**").hasAnyRole("ADMIN", "SALE")
                .requestMatchers("/admin/reviews/**").hasAnyRole("ADMIN", "SALE")

                // ─── ADMIN + WAREHOUSE (Sản phẩm, Danh mục, Thương hiệu, Kho) ───
                .requestMatchers("/admin/products/**").hasAnyRole("ADMIN", "WAREHOUSE")
                .requestMatchers("/admin/categories/**").hasAnyRole("ADMIN", "WAREHOUSE")
                .requestMatchers("/admin/brands/**").hasAnyRole("ADMIN", "WAREHOUSE")
                .requestMatchers("/admin/stock/**").hasAnyRole("ADMIN", "WAREHOUSE")

                // ─── BẤT KỲ ROLE ADMIN nào → có thể vào /admin/** còn lại ───
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SALE", "WAREHOUSE")

                // ─── CUSTOMER ROUTES (Bắt buộc đăng nhập) ───
                .requestMatchers("/cart/**", "/checkout/**", "/orders/**", "/user/**", "/account/**", "/wishlist/**").authenticated()

                // ─── PUBLIC ───
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/auth/login")
                .successHandler(successHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .clearAuthentication(true)
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}
