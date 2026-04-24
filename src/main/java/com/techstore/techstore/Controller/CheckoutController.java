package com.techstore.techstore.Controller;

import com.techstore.techstore.Service.*;
import com.techstore.techstore.entity.*;
import com.techstore.techstore.Repository.UserRepository;
import com.techstore.techstore.enums.PaymentMethod; //  THÊM PaymentMethod
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired private CartService cartService;
    @Autowired private OrderService orderService;
    @Autowired private ProductService productService;
    @Autowired private UserRepository userRepository;
    @Autowired private AddressService addressService;
    @Autowired private PaymentService paymentService;

    /**  Trang checkout chính — thanh toán toàn giỏ hàng */
    @GetMapping
    public String showCheckout(Model model, HttpServletRequest request, Principal principal) {
        if (principal == null) return "redirect:/login";

        CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
        model.addAttribute("_csrf", csrfToken);

        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Cart cart = cartService.getCartByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        Address defaultAddress = addressService.getDefaultAddress(user.getId());

        model.addAttribute("addresses", addressService.getAddressesByUser(user.getId()));
        model.addAttribute("defaultAddress", addressService.getDefaultAddress(user.getId()));

        model.addAttribute("user", user);
        model.addAttribute("cart", cart);
        model.addAttribute("pageTitle", "Thanh toán – TechStore");

        return "checkout";
    }

    /**  Trang checkout nhanh (Mua ngay) */
    @GetMapping("/buy-now")
    public String buyNow(@RequestParam Long productId,
                         @RequestParam(required = false) Long variantId,
                         @RequestParam(defaultValue = "1") int quantity,
                         Model model,
                         HttpServletRequest request,
                         Principal principal) {

        //  Chưa đăng nhập
        if (principal == null) return "redirect:/login";

        // CSRF
        CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
        if (csrfToken != null) model.addAttribute("_csrf", csrfToken);

        // Lấy user
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Lấy sản phẩm
        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID = " + productId));

        // Lấy biến thể nếu có chọn
        ProductVariant selectedVariant = null;
        if (variantId != null) {
            selectedVariant = product.getVariants()
                    .stream()
                    .filter(v -> v.getId().equals(variantId))
                    .findFirst()
                    .orElse(null);
        }

        // Lấy danh sách địa chỉ
        model.addAttribute("addresses", addressService.getAddressesByUser(user.getId()));
        model.addAttribute("defaultAddress", addressService.getDefaultAddress(user.getId()));

        // Gửi dữ liệu xuống view
        model.addAttribute("user", user);
        model.addAttribute("finalPrice", product.getFinalPrice());
        model.addAttribute("product", product);
        model.addAttribute("quantity", quantity);
        model.addAttribute("variantId", variantId);
        model.addAttribute("selectedVariant", selectedVariant); //  để hiển thị trong tóm tắt đơn hàng
        model.addAttribute("variants", product.getVariants());
        model.addAttribute("brand", product.getBrand());
        model.addAttribute("pageTitle", "Mua ngay – " + product.getName());

        return "checkout_buynow";
    }

    /** Xử lý đặt hàng (giỏ hàng hoặc mua ngay) */
    @PostMapping("/place")
    public String placeOrder(@RequestParam(required = false) Long variantId,

                             @RequestParam Long addressId,
                             @RequestParam PaymentMethod paymentMethod,
                             @RequestParam(required = false) String voucherCode,
                             @RequestParam(required = false) Long productId,
                             @RequestParam(required = false, defaultValue = "1") int quantity,
                             Principal principal) {

        if (principal == null) return "redirect:/login";

        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // lấy address trực tiếp
        Address address = addressService.getById(addressId);

        String fullAddress = String.join(", ",
                address.getAddressLine(),
                address.getWard(),
                address.getDistrict(),
                address.getCity()
        );
        String receiverName =address.getFullName();
        String receiverPhone =address.getPhone();


        Order order;
        // Mua ngay
        if (productId != null) {
            Product product = productService.getProductById(productId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

            ProductVariant variant = null;
            if (variantId != null) {
                variant = product.getVariants()
                        .stream()
                        .filter(v -> v.getId().equals(variantId))
                        .findFirst()
                        .orElse(null);
            }

            order=orderService.createOrderInstant(user, product,variant, quantity,
                    receiverName, fullAddress, receiverPhone, paymentMethod, voucherCode);
        }
        // Giỏ hàng
        else {
            order=orderService.createOrderFromCart(user,
                    receiverName, fullAddress, receiverPhone, paymentMethod, voucherCode);
        }

        //  Điều hướng theo phương thức thanh toán
        if (paymentMethod == PaymentMethod.COD) {
            return "redirect:/orders/success";
        }

        if (paymentMethod == PaymentMethod.SEPAY) {
            return "redirect:/checkout/sepay?orderId=" + order.getId();
        }

        return "redirect:/orders/success";

    }


    @GetMapping("/sepay")
    public String sepayPayment(@RequestParam Long orderId, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        Order order = orderService.getOrderById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Tạo nội dung chuyển khoản (mã đơn)
        String orderCode = order.getOrderCode(); // ví dụ DH102969

        // Tạo QR
        String qrUrl = paymentService.generatePaymentQR(
                order.getTotalAmount().longValue(),
                orderCode
        );

        model.addAttribute("order", order);
        model.addAttribute("qrUrl", qrUrl);
        model.addAttribute("pageTitle", "Thanh toán SePay – " + orderCode);

        return "payment_sepay"; // trang view
    }






}
