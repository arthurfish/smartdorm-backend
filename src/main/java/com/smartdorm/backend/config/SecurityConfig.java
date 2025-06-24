// src/main/java/com/smartdorm/backend/config/SecurityConfig.java
package com.smartdorm.backend.config;

import com.smartdorm.backend.security.JwtAccessDeniedHandler;
import com.smartdorm.backend.security.JwtAuthenticationEntryPoint;
import com.smartdorm.backend.security.JwtRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    public SecurityConfig(JwtRequestFilter jwtRequestFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          JwtAccessDeniedHandler jwtAccessDeniedHandler) {
        this.jwtRequestFilter = jwtRequestFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
    }

    // --- 现有 Bean 保持不变 ---
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // --- 安全链配置调整 ---

    @Bean
    @Order(1) // API 安全链，处理无状态的 JWT 请求
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**") // **关键**: 此链只处理 /api/** 下的请求
                .csrf(AbstractHttpConfigurer::disable) // **关键**: 对 API 禁用 CSRF
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/ping").permitAll() // 确保 API 登录端点是公开的
                        .requestMatchers("/api/auth/login").permitAll() // 确保 API 登录端点是公开的
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2) // 视图安全链，处理有状态的表单登录和页面访问
    public SecurityFilterChain formLoginFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 公开访问的资源
                        .requestMatchers("/css/**", "/js/**", "/error", "/ping", "/login", "/perform_login").permitAll()
                        // 权限控制
                        .requestMatchers("/view/admin/**").hasRole("ADMIN")
                        .requestMatchers("/view/student/**").hasRole("STUDENT")
                        // 其他任何请求都需要认证
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login") // 指定登录页面的 URL
                        .loginProcessingUrl("/perform_login") // 处理登录请求的 URL
                        .defaultSuccessUrl("/view/home", true) // 登录成功后的重定向 URL
                        .failureUrl("/login?error=true") // 登录失败后的 URL
                )
                .logout(logout -> logout
                        .logoutUrl("/perform_logout")
                        .logoutSuccessUrl("/login?logout=true")
                );
        // 注意：这里不需要禁用 CSRF，因为表单登录和 Thymeleaf 会处理它
        return http.build();
    }
}