package com.laptopstore.laptopstore.config;

import com.laptopstore.laptopstore.Repository.*;
import com.laptopstore.laptopstore.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "demo", havingValue = "true")
public class DemoDataSeeder {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final VoucherRepository voucherRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedDemoData() {
        return args -> {

            /* ===================== 1. ROLES ===================== */
            Role roleAdmin = upsertRole("ROLE_ADMIN", "Administrator");
            Role roleUser  = upsertRole("ROLE_USER", "Normal user");

            /* ===================== 2. ADMIN ===================== */
            upsertAdmin(roleAdmin);

            /* ===================== 3. BRANDS ===================== */
            Brand dell   = upsertBrand("Dell", "dell.png", "Laptop Dell chính hãng");
            Brand hp     = upsertBrand("HP", "hp.png", "Laptop HP chính hãng");
            Brand lenovo = upsertBrand("Lenovo", "lenovo.png", "Laptop Lenovo");
            Brand asus   = upsertBrand("ASUS", "asus.png", "Laptop ASUS");
            Brand acer   = upsertBrand("Acer", "acer.png", "Laptop Acer");
            Brand msi    = upsertBrand("MSI", "msi.png", "Laptop MSI");
            Brand apple  = upsertBrand("Apple", "apple.png", "MacBook chính hãng");
            Brand lg     = upsertBrand("LG", "lg.png", "Laptop LG Gram");

            /* ===================== 4. PRODUCTS ===================== */

            seedLaptop(
                    dell,
                    "Dell Inspiron 14 5430",
                    "DELL-INSP14-5430",
                    18990000,
                    "Laptop mỏng nhẹ cho học tập - văn phòng.",
                    "/images/products/dell-inspiron-14-5430-1.png," +
                    "/images/products/dell-inspiron-14-5430-2.png",
                    "16GB",
                    "14\" FHD+ IPS",
                    "Intel Core i5-1340P",
                    "Intel Iris Xe",
                    "54Wh",
                    "314 x 226 x 16.9 mm",
                    "Aluminum",
                    5,
                    "HOT",
                    List.of(
                            variant("Silver", "512GB", 20,
                                    "/images/products/dell-inspiron-14-5430-silver.png")
                    )
            );

            seedLaptop(
                    lenovo,
                    "Lenovo LOQ 15 (2024)",
                    "LEN-LOQ15-2024",
                    25990000,
                    "Gaming laptop màn 144Hz.",
                    "/images/products/lenovo-loq-15-2024-1.png," +
                    "/images/products/lenovo-loq-15-2024-2.png",
                    "16GB",
                    "15.6\" FHD 144Hz",
                    "Intel Core i5-12450H",
                    "RTX 4050",
                    "60Wh",
                    "359 x 264 x 23.9 mm",
                    "Plastic + Metal",
                    8,
                    "SALE",
                    List.of(
                            variant("Grey", "512GB", 15,
                                    "/images/products/lenovo-loq-15-2024-grey.png")
                    )
            );

            seedLaptop(
                    asus,
                    "ASUS TUF Gaming A15",
                    "ASUS-TUF-A15",
                    28990000,
                    "Laptop gaming chuẩn TUF.",
                    "/images/products/asus-tuf-a15-1.png," +
                    "/images/products/asus-tuf-a15-2.png",
                    "16GB",
                    "15.6\" FHD 144Hz",
                    "Ryzen 7 7735HS",
                    "RTX 4060",
                    "90Wh",
                    "354 x 251 x 24.9 mm",
                    "Plastic",
                    10,
                    "HOT",
                    List.of(
                            variant("Black", "1TB", 12,
                                    "/images/products/asus-tuf-a15-black.png")
                    )
            );

            seedLaptop(
                    hp,
                    "HP Pavilion 14",
                    "HP-PAV14",
                    17990000,
                    "Laptop văn phòng.",
                    "/images/products/hp-pavilion-14-1.png," +
                    "/images/products/hp-pavilion-14-2.png",
                    "16GB",
                    "14\" FHD",
                    "Intel Core i5-1335U",
                    "Intel Iris Xe",
                    "51Wh",
                    "324 x 214 x 17.9 mm",
                    "Aluminum",
                    0,
                    "NEW",
                    null
            );

            seedLaptop(
                    acer,
                    "Acer Aspire 5 A515",
                    "ACER-ASP5-A515",
                    15990000,
                    "Laptop phổ thông.",
                    "/images/products/acer-aspire-5-a515-1.png," +
                    "/images/products/acer-aspire-5-a515-2.png",
                    "8GB",
                    "15.6\" FHD",
                    "Intel Core i5-1235U",
                    "Intel Iris Xe",
                    "50Wh",
                    "362 x 238 x 17.9 mm",
                    "Plastic",
                    0,
                    null,
                    null
            );

            seedLaptop(
                    msi,
                    "MSI Katana 15",
                    "MSI-KATANA-15",
                    32990000,
                    "Laptop gaming MSI.",
                    "/images/products/msi-katana-15-1.png," +
                    "/images/products/msi-katana-15-2.png",
                    "16GB",
                    "15.6\" FHD 144Hz",
                    "Intel Core i7-13620H",
                    "RTX 4060",
                    "53.5Wh",
                    "359 x 259 x 24.9 mm",
                    "Plastic",
                    7,
                    "SALE",
                    null
            );

            seedLaptop(
                    apple,
                    "MacBook Air M2 13.6",
                    "APPLE-MBA-M2-13",
                    24990000,
                    "MacBook Air M2.",
                    "/images/products/macbook-air-m2-13-1.png," +
                    "/images/products/macbook-air-m2-13-2.png",
                    "8GB",
                    "13.6\" Liquid Retina",
                    "Apple M2",
                    "Apple GPU",
                    "52.6Wh",
                    "304 x 215 x 11.3 mm",
                    "Aluminum",
                    0,
                    "HOT",
                    List.of(
                            variant("Midnight", "256GB", 10,
                                    "/images/products/macbook-air-m2-13-midnight.png"),
                            variant("Starlight", "512GB", 5,
                                    "/images/products/macbook-air-m2-13-starlight.png")
                    )
            );

            seedLaptop(
                    apple,
                    "MacBook Pro M3 14",
                    "APPLE-MBP-M3-14",
                    39990000,
                    "MacBook Pro M3.",
                    "/images/products/macbook-pro-m3-14-1.png," +
                    "/images/products/macbook-pro-m3-14-2.png",
                    "16GB",
                    "14.2\" XDR",
                    "Apple M3",
                    "Apple GPU",
                    "70Wh",
                    "312 x 221 x 15.5 mm",
                    "Aluminum",
                    0,
                    "NEW",
                    null
            );

            seedLaptop(
                    lg,
                    "LG Gram 14",
                    "LG-GRAM-14",
                    31990000,
                    "Laptop siêu nhẹ.",
                    "/images/products/lg-gram-14-1.png," +
                    "/images/products/lg-gram-14-2.png",
                    "16GB",
                    "14\" WUXGA",
                    "Intel Core i7-1360P",
                    "Intel Iris Xe",
                    "72Wh",
                    "313 x 215 x 17.3 mm",
                    "Magnesium alloy",
                    0,
                    null,
                    null
            );

            seedLaptop(
                    dell,
                    "Dell XPS 13",
                    "DELL-XPS-13",
                    35990000,
                    "Ultrabook cao cấp.",
                    "/images/products/dell-xps-13-1.png," +
                    "/images/products/dell-xps-13-2.png",
                    "16GB",
                    "13.4\" FHD+",
                    "Intel Core i7-1355U",
                    "Intel Iris Xe",
                    "55Wh",
                    "296 x 199 x 15.3 mm",
                    "Aluminum",
                    5,
                    "SALE",
                    null
            );

            seedLaptop(
                    lenovo,
                    "ThinkPad X1 Carbon",
                    "LEN-X1C",
                    42990000,
                    "Laptop doanh nhân.",
                    "/images/products/thinkpad-x1-carbon-1.png," +
                    "/images/products/thinkpad-x1-carbon-2.png",
                    "16GB",
                    "14\" WUXGA",
                    "Intel Core i7-1365U",
                    "Intel Iris Xe",
                    "57Wh",
                    "315 x 223 x 15.4 mm",
                    "Carbon fiber",
                    0,
                    null,
                    null
            );

            seedLaptop(
                    asus,
                    "ASUS Vivobook 15",
                    "ASUS-VIVO-15",
                    13990000,
                    "Laptop phổ thông.",
                    "/images/products/asus-vivobook-15-1.png," +
                    "/images/products/asus-vivobook-15-2.png",
                    "16GB",
                    "15.6\" FHD",
                    "Ryzen 5 7530U",
                    "Radeon Graphics",
                    "42Wh",
                    "359 x 232 x 19.9 mm",
                    "Plastic",
                    0,
                    null,
                    null
            );

            /* ===================== 5. VOUCHERS ===================== */
            upsertVoucher(
                    "WELCOME10",
                    new BigDecimal("10"),
                    "PERCENT",
                    new BigDecimal("5000000"),
                    LocalDateTime.now().minusDays(1),
                    LocalDateTime.now().plusDays(90),
                    100,
                    true,
                    "Giảm 10% cho đơn hàng từ 5.000.000đ"
            );
        };
    }

    private Role upsertRole(String name, String description) {
        return roleRepository.findByName(name)
                .map(r -> {
                    r.setDescription(description);
                    return roleRepository.save(r);
                })
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setName(name);
                    r.setDescription(description);
                    return roleRepository.save(r);
                });
    }

private void upsertAdmin(Role adminRole) {
    User admin = userRepository.findByUsername("admin")
            .orElseGet(() -> {
                User u = new User();
                u.setUsername("admin");
                u.setEmail("admin@techstore.com");
                return u;
            });

    admin.setFullName("Administrator");
    admin.setPhone("0000000000");
    admin.setDeleted(false);

    // 🔥 LUÔN reset password
    admin.setPassword(passwordEncoder.encode("admin123"));

    admin.setRoles(Set.of(adminRole));
    userRepository.save(admin);
}


    private Brand upsertBrand(String name, String logoFileName, String description) {
        return brandRepository.findByName(name)
                .map(b -> {
                    b.setLogo(logoFileName);
                    b.setDescription(description);
                    b.setActive(true);
                    return brandRepository.save(b);
                })
                .orElseGet(() -> {
                    Brand b = new Brand();
                    b.setName(name);
                    b.setLogo(logoFileName);
                    b.setDescription(description);
                    b.setActive(true);
                    return brandRepository.save(b);
                });
    }

    private static record VariantSpec(String color, String storage, int stock, String image) {}

    private static VariantSpec variant(String color, String storage, int stock, String image) {
        return new VariantSpec(color, storage, stock, image);
    }

    private void seedLaptop(
            Brand brand,
            String name,
            String model,
            long priceVnd,
            String description,
            String imagesCsv,
            String ram,
            String display,
            String cpu,
            String gpu,
            String battery,
            String dimensions,
            String material,
            int salePercent,
            String badge,
            List<VariantSpec> variants
    ) {
        if (productRepository.findByModel(model).isPresent()) return;

        Product p = new Product();
        p.setName(name);
        p.setModel(model);
        p.setBrand(brand);
        p.setPrice(BigDecimal.valueOf(priceVnd));
        p.setDescription(description);
        p.setImages(imagesCsv);

        p.setRam(ram);
        p.setDisplay(display);
        p.setCpu(cpu);
        p.setGpu(gpu);
        p.setBattery(battery);
        p.setDimensions(dimensions);
        p.setMaterial(material);

        p.setSalePercent(salePercent);
        p.setBadge(badge);
        p.setDeleted(false);

        // Variants
        if (variants != null) {
            for (VariantSpec vs : variants) {
                ProductVariant v = new ProductVariant();
                v.setColor(vs.color());
                v.setStorage(vs.storage());
                v.setStock(vs.stock());
                v.setImage(vs.image());
                p.addVariant(v);
            }
        }

        productRepository.save(p);
    }

    private void upsertVoucher(
            String code,
            BigDecimal discountValue,
            String discountType,
            BigDecimal minOrderValue,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int quantity,
            boolean active,
            String description
    ) {
        Voucher v = voucherRepository.findByCode(code)
                .orElseGet(() -> {
                    Voucher nv = new Voucher();
                    nv.setCode(code);
                    return nv;
                });

        v.setDiscountValue(discountValue);
        v.setDiscountType(discountType);
        v.setMinOrderValue(minOrderValue);
        v.setStartDate(startDate);
        v.setEndDate(endDate);
        v.setQuantity(quantity);
        v.setActive(active);
        v.setDescription(description);

        voucherRepository.save(v);
    }
}
