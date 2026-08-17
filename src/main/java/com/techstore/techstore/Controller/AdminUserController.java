package com.techstore.techstore.Controller;

import com.techstore.techstore.Service.UserService;
import com.techstore.techstore.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Controller
@RequestMapping("/admin/users")
public class AdminUserController {
    @Autowired
    private UserService userService;

    @GetMapping
    public String listUser(Model model){
        List<User> users = userService.getAllUser();
        model.addAttribute("users", users);
        model.addAttribute("pageTitle", "Quản lý Người dùng - LaptopStore Admin");
        model.addAttribute("active", "users");
        return "admin/users";
    }

    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id){
        userService.deleteUser(id);
        return "redirect:/admin/users?deleted=true";
    }

    @PostMapping("/edit/{id}")
    public String editUser (@PathVariable Long id,
                            @RequestParam String fullName,
                            @RequestParam String email,
                            @RequestParam(required = false) String phone
    ){
        User user = userService.getUserById(id).orElse(null);
        if(user == null) return "redirect:/admin/users?error=notfound";

        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);

        userService.saveUser(user);
        return "redirect:/admin/users?updated=true";
    }

}
