package com.medico.web;

import com.medico.service.Dtos.*;
import com.medico.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService svc;
    public ProfileController(ProfileService svc) { this.svc = svc; }

    @GetMapping public ProfileDto get(@AuthenticationPrincipal Long uid) { return svc.get(uid); }
    @PutMapping public ProfileDto update(@AuthenticationPrincipal Long uid, @Valid @RequestBody ProfileReq r) { return svc.update(uid, r); }
    @PostMapping("/password") public void password(@AuthenticationPrincipal Long uid, @Valid @RequestBody PasswordReq r) { svc.changePassword(uid, r); }
}
