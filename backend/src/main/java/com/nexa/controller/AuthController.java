package com.nexa.controller;

import com.nexa.config.JwtUtil;
import com.nexa.dto.AuthDtos.*;
import com.nexa.model.User;
import com.nexa.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            return ResponseEntity.badRequest().body("Un compte existe deja avec cet email.");
        }
        User user = new User(req.email(), passwordEncoder.encode(req.password()), req.fullName());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getFullName(), user.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Email ou mot de passe incorrect.");
        }

        User user = userRepository.findByEmail(req.email()).orElseThrow();
        String token = jwtUtil.generateToken(user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getFullName(), user.getEmail()));
    }
}
