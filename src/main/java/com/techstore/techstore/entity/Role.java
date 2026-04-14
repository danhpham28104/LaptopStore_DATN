package com.techstore.techstore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 *  Bảng Role lưu vai trò người dùng, ví dụ: ADMIN, USER, MANAGER
 * Có thể mở rộng thêm mô tả, trạng thái (active), timestamps.
 */
@Entity
@Table(
        name = "roles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_roles_name", columnNames = "name")
        },
        indexes = {
                @Index(name = "idx_roles_name", columnList = "name")
        }
)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //  Tên role duy nhất, luôn viết hoa (ADMIN, USER, MANAGER...)
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    //  Mô tả giúp quản trị viên dễ hiểu chức năng của role
    @Size(max = 255)
    @Column(length = 255)
    private String description;


    //  Quan hệ ngược với User: không cascade REMOVE để tránh xoá user khi xoá role
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @JsonIgnore // tránh vòng lặp JSON khi serialize
    private Set<User> users = new HashSet<>();

    // ===== Constructors =====
    public Role() {}

    public Role(Long id, String name) {
        this.id = id;
        this.name = name != null ? name.toUpperCase() : null; // 🔧 Normalize name
    }



    // ===== Lifecycle hook =====
    @PrePersist
    @PreUpdate
    private void normalizeName() {
        //  Đảm bảo tên role luôn in hoa, không khoảng trắng
        if (this.name != null) this.name = this.name.trim().toUpperCase();
    }

    // ===== Getters & Setters =====
    public Long getId() {
        return id;
    }

    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) {
        this.name = (name != null) ? name.trim().toUpperCase() : null;
    }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }


    public Set<User> getUsers() { return users; }

    public void setUsers(Set<User> users) {
        this.users = (users != null) ? users : new HashSet<>();
    }

    // ===== Helper methods =====
    public void addUser(User user) {
        users.add(user);
    }

    public void removeUser(User user) {
        users.remove(user);
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", users=" + users +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        return id != null && id.equals(role.getId());
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
