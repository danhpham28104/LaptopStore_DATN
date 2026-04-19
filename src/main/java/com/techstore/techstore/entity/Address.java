package com.techstore.techstore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Họ tên người nhận */
    @NotBlank(message = "Tên người nhận không được để trống")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /** Số điện thoại */
    @NotBlank(message = "Số điện thoại không được để trống")
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    /** Địa chỉ chi tiết */
    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255)
    @Column(name = "address_line", nullable = false, length = 255)
    private String addressLine;

    /** Phường/xã */
    @Column(length = 100)
    private String ward;

    /** Quận/huyện */
    @Column(length = 100)
    private String district;

    /** Tỉnh/thành phố */
    @Column(length = 100)
    private String city;

    /** Đặt làm mặc định */
    @Column(name = "is_default")
    private boolean isDefault = false;

    /** Khóa ngoại User */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // ===== Constructors =====
    public Address() {}

    public Address(String fullName, String phone, String addressLine, String ward,
                   String district, String city, boolean isDefault, User user) {
        this.fullName = fullName;
        this.phone = phone;
        this.addressLine = addressLine;
        this.ward = ward;
        this.district = district;
        this.city = city;
        this.isDefault = isDefault;
        this.user = user;
    }

    // ===== Getters & Setters =====
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public void setId(Long id) {
        this.id = id;
    }

    // ===== ToString (ẩn user để tránh vòng lặp JSON) =====
    @Override
    public String toString() {
        return "Address{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", phone='" + phone + '\'' +
                ", addressLine='" + addressLine + '\'' +
                ", ward='" + ward + '\'' +
                ", district='" + district + '\'' +
                ", city='" + city + '\'' +
                ", isDefault=" + isDefault +
                '}';
    }
}
