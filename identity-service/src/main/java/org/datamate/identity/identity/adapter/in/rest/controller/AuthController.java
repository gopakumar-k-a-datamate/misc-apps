package org.datamate.identity.identity.adapter.in.rest.controller;

import com.datamate.bedrock.framework.common.auditing.annotation.AuditLog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.datamate.identity.identity.application.dto.auth.AuthResponse;
import org.datamate.identity.identity.application.dto.auth.LoginRequest;
import org.datamate.identity.identity.application.dto.auth.RefreshTokenRequest;
import org.datamate.identity.identity.application.port.in.auth.LoginUseCase;
import org.datamate.identity.identity.application.port.in.auth.RefreshTokenUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;

    @PostMapping("/login")
    @AuditLog(action = "USER_LOGIN", resource = "AUTH", description = "User login attempt", includeArgs = true)
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = loginUseCase.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @AuditLog(action = "USER_LOGOUT", resource = "AUTH", description = "User logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        loginUseCase.logout(authHeader);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    @AuditLog(action = "TOKEN_REFRESH", resource = "AUTH", description = "Refresh access token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = refreshTokenUseCase.refreshToken(request);
        return ResponseEntity.ok(response);
    }
}
