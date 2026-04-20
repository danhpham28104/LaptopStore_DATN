package com.techstore.techstore.Repository;

import com.techstore.techstore.entity.Address;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUser_Id(Long userId);

    boolean existsByUser_IdAndIsDefaultTrue(Long userId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId")
    void clearDefaultByUser(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = true WHERE a.id = :id")
    void setDefault(@Param("id") Long id);

    Address findByUser_IdAndIsDefaultTrue(Long userId);
}
