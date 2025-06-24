package com.smartdorm.backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    /**
     * 显示自定义的登录页面。
     * 路径与 SecurityConfig 中的 .loginPage("/login") 对应。
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login"; // 返回 templates/login.html
    }

    /**
     * 登录成功后的统一入口点。
     * 根据用户角色重定向到各自的仪表盘。
     * 路径与 SecurityConfig 中的 .defaultSuccessUrl("/view/home", true) 对应。
     */
    @GetMapping(value = {"/", "/view/home"})
    public String home(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        // 检查用户角色并重定向
        for (GrantedAuthority auth : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(auth.getAuthority())) {
                return "redirect:/view/admin/dashboard";
            }
            if ("ROLE_STUDENT".equals(auth.getAuthority())) {
                return "redirect:/view/student/dashboard";
            }
        }

        // 默认回退（理论上不应发生）
        return "redirect:/login?error=true";
    }
}