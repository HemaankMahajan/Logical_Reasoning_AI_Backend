package com.AI.writer.app.API;

import com.AI.writer.app.Util.JwtUtil;
import com.AI.writer.app.Entity.User;
import com.AI.writer.app.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String email = request.get("email");
        String password = request.get("password");

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is already registered!"));
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .build();

        userRepository.save(user);

        // Pass user.getEmail() AND user.getId() to generateToken
        String token = jwtUtil.generateToken(user.getEmail(), user.getId());

        return ResponseEntity.ok(Map.of(
                "message", "User registered successfully!",
                "token", token,
                "name", user.getName(),
                "email", user.getEmail()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password!"));
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password!"));
        }

        // Pass user.getEmail() AND user.getId() to generateToken
        String token = jwtUtil.generateToken(user.getEmail(), user.getId());

        return ResponseEntity.ok(Map.of(
                "message", "Login successful!",
                "token", token,
                "name", user.getName(),
                "email", user.getEmail()
        ));
    }
    @PutMapping("/update-name")
    public ResponseEntity<?> updateName(
            @RequestAttribute("userId") Long userId,
            @RequestBody Map<String, String> request) {
        String newName = request.get("name");
        if (newName == null || newName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name cannot be empty."));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found."));
        }

        User user = userOpt.get();
        user.setName(newName.trim());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Name updated successfully!",
                "name", user.getName()
        ));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestAttribute("userId") Long userId,
            @RequestBody Map<String, String> request) {
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");

        if (currentPassword == null || newPassword == null || newPassword.trim().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password must be at least 6 characters."));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found."));
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Current password is incorrect."));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password updated successfully!"));
    }
}