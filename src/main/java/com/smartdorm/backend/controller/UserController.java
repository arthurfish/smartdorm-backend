// src/main/java/com/smartdorm/backend/controller/UserController.java
package com.smartdorm.backend.controller;

import com.smartdorm.backend.dto.UserDto;
import com.smartdorm.backend.entity.User;
import com.smartdorm.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to handle user-related operations.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retrieves the details of the currently authenticated user.
     * This is a protected endpoint.
     *
     * @param userDetails The details of the authenticated user, injected by Spring Security.
     * @return A DTO with the current user's information.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        // Find the full user entity from the database using the username from the token
        User user = userRepository.findByStudentId(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));

        // Map the entity to a safe DTO (without password)
        UserDto userDto = new UserDto(user.getId(), user.getStudentId(), user.getName(), user.getRole(), user.getGender(), user.getCollege());
        return ResponseEntity.ok(userDto);
    }
}