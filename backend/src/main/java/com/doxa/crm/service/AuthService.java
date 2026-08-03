package com.doxa.crm.service;

import com.doxa.crm.domain.entity.User;
import com.doxa.crm.dto.auth.LoginRequest;
import com.doxa.crm.dto.auth.LoginResponse;
import com.doxa.crm.dto.auth.UserResponse;
import com.doxa.crm.repository.UserRepository;
import com.doxa.crm.security.AuthUser;
import com.doxa.crm.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        String token = jwtService.generateToken(authUser);

        User user = userRepository.findByEmailIgnoreCase(authUser.getEmail())
                .orElseThrow();

        return new LoginResponse(
                token,
                "Bearer",
                toUserResponse(user)
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(AuthUser authUser) {
        User user = userRepository.findByEmailIgnoreCase(authUser.getEmail())
                .orElseThrow();
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getLicense().getId(),
                user.getEmail(),
                user.getRole(),
                user.getLicense().getCompanyName()
        );
    }
}
