package com.smartdorm.backend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/view")
public class DashboardController {

    /**
     * Handles requests for the admin dashboard.
     * Only accessible by users with the 'ADMIN' role.
     * @return The view name for the admin dashboard.
     */
    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Model model) {
        // Here you can add data from services to the model
        // For example: model.addAttribute("pendingRequests", swapRequestService.countPending());
        return "admin/dashboard"; // Corresponds to /templates/admin/dashboard.html
    }

    /**
     * Handles requests for the student dashboard.
     * Only accessible by users with the 'STUDENT' role.
     * @return The view name for the student dashboard.
     */
    @GetMapping("/student/dashboard")
    @PreAuthorize("hasRole('STUDENT')")
    public String studentDashboard(Model model) {
        // Example: model.addAttribute("isSurveyOpen", surveyService.isSurveyOpenForStudent());
        return "student/dashboard"; // Corresponds to /templates/student/dashboard.html
    }
}