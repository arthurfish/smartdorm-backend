package com.smartdorm.backend.controller;

import com.smartdorm.backend.dto.StudentDtos.*;
import com.smartdorm.backend.entity.User;
import com.smartdorm.backend.exception.ResourceNotFoundException;
import com.smartdorm.backend.repository.UserRepository;
import com.smartdorm.backend.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final StudentService studentService;
    private final UserRepository userRepository;

    public StudentController(StudentService studentService, UserRepository userRepository) {
        this.studentService = studentService;
        this.userRepository = userRepository;
    }

    @GetMapping("/survey")
    public ResponseEntity<SurveyForStudentDto> getSurvey() {
        return ResponseEntity.ok(studentService.getSurveyForStudent());
    }

    @PostMapping("/responses")
    public ResponseEntity<Void> submitResponses(@Valid @RequestBody UserResponseSubmitDto dto, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = getCurrentUser(userDetails);
        studentService.submitResponses(dto, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/result")
    public ResponseEntity<AssignmentResultStudentDto> getResult(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = getCurrentUser(userDetails);
        return ResponseEntity.ok(studentService.getStudentResult(currentUser));
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByStudentId(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database."));
    }
}