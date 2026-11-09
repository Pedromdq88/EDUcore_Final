package com.educore.sge.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/** Anonymous recovery endpoints; identity is proven only by the emailed one-time code. */
@RestController
@RequestMapping("/api/v1/auth")
public class PasswordRecoveryController {
    private final PasswordRecoveryService recovery;

    public PasswordRecoveryController(PasswordRecoveryService recovery) {
        this.recovery = recovery;
    }

    @PostMapping("/forgot-password")
    public PasswordRecoveryService.Message requestCode(
            @RequestBody PasswordRecoveryService.Request request,
            HttpServletRequest http) {
        // The remote address is used for a local rate limit; never trust client-supplied X-Forwarded-For.
        return recovery.request(request, http.getRemoteAddr());
    }

    @PostMapping("/verify-reset-code")
    public PasswordRecoveryService.VerifyResult verifyCode(
            @RequestBody PasswordRecoveryService.Verify request) {
        return recovery.verify(request);
    }

    @PostMapping("/reset-password")
    public PasswordRecoveryService.Message confirmCode(
            @RequestBody PasswordRecoveryService.Confirm request) {
        return recovery.confirm(request);
    }
}
