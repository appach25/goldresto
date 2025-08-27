package com.goldresto.controller;

import com.goldresto.entity.Role;
import com.goldresto.entity.User;
import com.goldresto.repository.RoleRepository;
import com.goldresto.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('OWNER')")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "users/list";
    }

    @GetMapping("/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("allRoles", roleRepository.findAll());
        return "users/form";
    }

    @PostMapping("/create")
    public String createUser(@ModelAttribute User user, @RequestParam List<Long> roles) {
        userService.createUserWithRoles(user, roles);
        return "redirect:/users";
    }

    @GetMapping("/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        model.addAttribute("allRoles", roleRepository.findAll());
        return "users/form";
    }

    @PostMapping("/update/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute User user, @RequestParam List<Long> roles) {
        userService.updateUser(id, user, roles);
        return "redirect:/users";
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public String toggleUserStatus(@PathVariable Long id) {
        boolean newStatus = userService.toggleUserStatus(id);
        return String.valueOf(newStatus);
    }
}
