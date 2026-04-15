package com.techstore.techstore.Repository;

import com.techstore.techstore.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    //  Trả về Optional cho an toàn
    Optional<Role> findByName(String nameRole);
    boolean existsByName(String nameRole);
}
