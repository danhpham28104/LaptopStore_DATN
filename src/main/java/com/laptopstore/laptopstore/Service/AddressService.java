package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.AddressRepository;
import com.laptopstore.laptopstore.Repository.UserRepository;
import com.laptopstore.laptopstore.entity.Address;
import com.laptopstore.laptopstore.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Lấy danh sách địa chỉ của 1 user
     */
    public List<Address> getAddressesByUser(Long userId) {
        return addressRepository.findByUser_Id(userId);
    }

    /**
     * Thêm địa chỉ mới cho user
     */
    @Transactional
    public void addAddress(Long userId, Address address) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        address.setUser(user);

        // Nếu user chưa có địa chỉ nào → đặt mặc định
        List<Address> existing = addressRepository.findByUser_Id(userId);
        if (existing.isEmpty()) {
            address.setDefault(true);
        }

        addressRepository.save(address);
    }

    /**
     * Xóa địa chỉ
     */
    @Transactional
    public void deleteAddress(Long addressId) {
        addressRepository.deleteById(addressId);
    }

    /**
     * Đặt địa chỉ làm mặc định
     */
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        // Bỏ mặc định tất cả địa chỉ khác
        List<Address> addresses = addressRepository.findByUser_Id(userId);
        for (Address addr : addresses) {
            addr.setDefault(addr.getId().equals(addressId));
        }
        addressRepository.saveAll(addresses);
    }

    /**
     * Lấy địa chỉ mặc định của user
     */
    public Address getDefaultAddress(Long userId) {
        return addressRepository.findByUser_IdAndIsDefaultTrue(userId);
    }

    public Address getById(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ ID = " + id));
    }
}
