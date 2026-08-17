package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.RoleRepository;
import com.laptopstore.laptopstore.entity.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RoleService {
    @Autowired
    private RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Role> getByName(String nameRole) {
        return roleRepository.findByName(nameRole);
    }

    @Transactional
    public Role save(Role role) {
        return roleRepository.save(role);
    }
}
