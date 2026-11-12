package com.educore.sge.auth.web;

import com.educore.sge.auth.mail.EmailProofService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/verify-contact-email")
@PreAuthorize("hasAnyRole('DIRECTOR','ADMINISTRATIVE')")
public class EmailProofController {
    private final EmailProofService verification;
    public EmailProofController(EmailProofService verification){this.verification=verification;}
    public record CodeRequest(String email) {}
    public record CodeVerify(String email,String code) {}
    public record Result(boolean valid,String message) {}

    @PostMapping("/request")
    public Result request(@RequestHeader("X-Institution-Id") String tenant,
                         Authentication authentication,@RequestBody CodeRequest payload){
        verification.request(tenant,authentication.getName(),payload.email());
        return new Result(false,"Se envió un código de seis dígitos a la dirección indicada.");
    }
    @PostMapping("/confirm")
    public Result verify(@RequestHeader("X-Institution-Id") String tenant,
                        Authentication authentication,@RequestBody CodeVerify payload){
        boolean result=verification.verify(tenant,authentication.getName(),payload.email(),payload.code());
        return new Result(result,result ? "Correo verificado. Ahora podés guardar los cambios." :
                "Código incorrecto o vencido. Revisalo e intentá nuevamente.");
    }
}
