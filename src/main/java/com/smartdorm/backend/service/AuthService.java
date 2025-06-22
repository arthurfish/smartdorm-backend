package com.smartdorm.backend.service;

import com.smartdorm.backend.dto.*;
import com.smartdorm.backend.entity.User;
import com.smartdorm.backend.repository.UserRepository;
import com.smartdorm.backend.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AuthService(AuthenticationManager authenticationManager, UserDetailsService userDetailsService, UserRepository userRepository, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.studentId(), loginRequest.password())
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid student ID or password");
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.studentId());
        final String token = jwtUtil.generateToken(userDetails);

        // Fetch user entity to create UserDto
        User user = userRepository.findByStudentId(loginRequest.studentId()).orElseThrow(() -> new UsernameNotFoundException("User not found after successful authentication for: " + loginRequest.studentId()));
        UserDto userDto = new UserDto(user.getId(), user.getStudentId(), user.getName(), user.getRole(), user.getGender(), user.getCollege());

        return new LoginResponse(token, userDto);
    }
}