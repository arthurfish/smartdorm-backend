package com.smartdorm.backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// src/main/java/com/smartdorm/backend/controller/ViewController.java
@Controller
public class ViewController {
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping(value = {"/", "/view/home"})
    public String home(Authentication authentication) {
        // 根据角色重定向到不同的主页
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/view/admin/dashboard";
        }
        return "redirect:/view/student/dashboard";
    }
}