package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Repository.UserRepository;
import com.laptopstore.laptopstore.Service.AddressService;
import com.laptopstore.laptopstore.entity.Address;
import com.laptopstore.laptopstore.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/addresses")
public class AddressApiController {

    private final AddressService addressService;
    private final UserRepository userRepository;

    public AddressApiController(AddressService addressService, UserRepository userRepository) {
        this.addressService = addressService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> createAddress(@Valid @RequestBody Address address, Principal principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập để thực hiện thao tác này");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElse(null);

        if (user == null) {
            response.put("success", false);
            response.put("message", "Không tìm thấy thông tin tài khoản người dùng");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Address saved = addressService.addAddress(user.getId(), address);

        response.put("success", true);
        response.put("message", "Thêm địa chỉ mới thành công!");
        response.put("address", saved);

        return ResponseEntity.ok(response);
    }
}
