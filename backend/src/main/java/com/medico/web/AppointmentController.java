package com.medico.web;

import com.medico.service.AppointmentService;
import com.medico.service.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService svc;
    public AppointmentController(AppointmentService svc) { this.svc = svc; }

    @PostMapping public BookRes book(@AuthenticationPrincipal Long uid, @Valid @RequestBody BookReq r) { return svc.book(uid, r); }
    @GetMapping("/mine") public List<AppointmentDto> mine(@AuthenticationPrincipal Long uid) { return svc.mine(uid); }
    @GetMapping("/{id}") public AppointmentDto get(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return svc.get(uid, id); }

    @PostMapping("/{id}/pay/verify")
    public AppointmentDto verify(@AuthenticationPrincipal Long uid, @PathVariable Long id, @Valid @RequestBody VerifyReq v) { return svc.verifyPayment(uid, id, v); }

    @PostMapping("/{id}/pay/demo")
    public AppointmentDto demo(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return svc.demoPay(uid, id); }

    @PostMapping("/{id}/cancel") public AppointmentDto cancel(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return svc.cancel(uid, id); }

    @PostMapping("/{id}/review")
    public AppointmentDto review(@AuthenticationPrincipal Long uid, @PathVariable Long id, @Valid @RequestBody ReviewReq r) { return svc.review(uid, id, r); }
}
