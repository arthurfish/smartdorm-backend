package com.smartdorm.backend.config;

import com.smartdorm.backend.entity.User;
import com.smartdorm.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Create a default admin user if not exists
        if (userRepository.findByStudentId("admin").isEmpty()) {
            User adminUser = new User();
            adminUser.setStudentId("admin");
            adminUser.setName("Administrator");
            // IMPORTANT: The password is "password"
            adminUser.setPassword(passwordEncoder.encode("password"));
            adminUser.setRole("ADMIN");
            adminUser.setGender("MALE");
            adminUser.setCollege("System Administration");
            userRepository.save(adminUser);
            System.out.println("====== Default admin user created. Username: admin, Password: password ======");
        }
        if (userRepository.findByStudentId("student").isEmpty()) {
            User adminUser = new User();
            adminUser.setStudentId("student");
            adminUser.setName("Student");
            // IMPORTANT: The password is "password"
            adminUser.setPassword(passwordEncoder.encode("password"));
            adminUser.setRole("STUDENT");
            adminUser.setGender("MALE");
            adminUser.setCollege("CS");
            userRepository.save(adminUser);
            System.out.println("====== Default student user created. Username: student, Password: password ======");
        }
    }
}