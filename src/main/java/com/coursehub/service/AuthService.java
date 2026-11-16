package com.coursehub.service;

import com.coursehub.dto.request.LoginRequest;
import com.coursehub.dto.request.RegisterRequest;
import com.coursehub.dto.response.AuthResponse;
import com.coursehub.dto.response.UserResponse;
import com.coursehub.entity.BlacklistedToken;
import com.coursehub.entity.User;
import com.coursehub.entity.enums.UserRole;
import com.coursehub.exception.ConflictException;
import com.coursehub.mapper.UserMapper;
import com.coursehub.repository.BlacklistedTokenRepository;
import com.coursehub.repository.UserRepository;
import com.coursehub.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        // The phone number is optional. An empty value is saved as null, so the unique rule allows many users without one.
        String phone = (request.getPhoneNumber() == null || request.getPhoneNumber().isEmpty())
                ? null : request.getPhoneNumber();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already registered");
        }
        if (phone != null && userRepository.existsByPhoneNumber(phone)) {
            throw new ConflictException("Phone number is already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(phone)
                .role(UserRole.LEARNER)
                .build();
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new DisabledException("Account is disabled");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, "Bearer", jwtService.getExpirationHours(), UserMapper.toResponse(user));
    }


    @Transactional
    public void logout(String token) {
        blacklistedTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now()); // remove old expired rows
        if (!blacklistedTokenRepository.existsByToken(token)) {
            LocalDateTime expiresAt = LocalDateTime.ofInstant(
                    jwtService.extractExpiration(token).toInstant(), ZoneId.systemDefault());
            blacklistedTokenRepository.save(BlacklistedToken.builder().token(token).expiresAt(expiresAt).build());
        }
    }
}
