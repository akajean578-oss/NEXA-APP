package com.nexa.controller;

import com.nexa.config.JwtUtil;
import com.nexa.dto.AuthDtos.LoginRequest;
import com.nexa.dto.AuthDtos.RegisterRequest;
import com.nexa.model.User;
import com.nexa.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks private AuthController authController;

    @Test
    void register_refuseSiEmailDejaUtilise() {
        when(userRepository.existsByEmail("dejapris@nexa.dev")).thenReturn(true);

        ResponseEntity<?> response = authController.register(
                new RegisterRequest("dejapris@nexa.dev", "motdepasse123", "Quelqu'un"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_creeLeCompteEtRetourneUnToken() throws Exception {
        when(userRepository.existsByEmail("nouveau@nexa.dev")).thenReturn(false);
        when(passwordEncoder.encode("motdepasse123")).thenReturn("hashed");

        User saved = new User("nouveau@nexa.dev", "hashed", "Nouveau User");
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(saved, 42L);
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtUtil.generateToken("nouveau@nexa.dev")).thenReturn("fake.jwt.token");

        ResponseEntity<?> response = authController.register(
                new RegisterRequest("nouveau@nexa.dev", "motdepasse123", "Nouveau User"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(jwtUtil).generateToken("nouveau@nexa.dev");
    }

    @Test
    void login_refuseSiIdentifiantsInvalides() {
        doThrow(new RuntimeException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        ResponseEntity<?> response = authController.login(
                new LoginRequest("inconnu@nexa.dev", "mauvaispass"));

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void login_retourneUnTokenSiIdentifiantsValides() {
        User user = new User("ok@nexa.dev", "hashed", "Utilisateur OK");
        when(userRepository.findByEmail("ok@nexa.dev")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("ok@nexa.dev")).thenReturn("fake.jwt.token");

        ResponseEntity<?> response = authController.login(
                new LoginRequest("ok@nexa.dev", "bonpass"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(authenticationManager).authenticate(any());
    }
}
