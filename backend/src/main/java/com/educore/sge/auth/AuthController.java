package com.educore.sge.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }
    public record LoginRequest(String institutionId, String email, String password) {}
    public record ForgotRequest(String institutionId, String email) {}
    public record ResetRequest(String token, String newPassword) {}
    public record ChangeRequest(String currentPassword, String newPassword) {}
    @PostMapping("/login")
    public AuthService.LoginResult login(@RequestBody LoginRequest req) {
        return service.login(req.institutionId(),req.email(),req.password());
    }
    @GetMapping("/me")
    public AuthPrincipal me() { return service.principal(); }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value="Authorization",required=false) String bearer) {
        service.logout(bearer); return ResponseEntity.noContent().build();
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgot(@RequestBody ForgotRequest req) {
        service.forgotPassword(req.institutionId(), req.email());
        return ResponseEntity.accepted().build();
    }
    @PostMapping("/reset-password")
    public ResponseEntity<Void> reset(@RequestBody ResetRequest req) {
        service.resetPassword(req.token(),req.newPassword());
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/change-password")
    public ResponseEntity<Void> change(@RequestBody ChangeRequest req) {
        service.changePassword(req.currentPassword(),req.newPassword());
        return ResponseEntity.noContent().build();
    }
}
