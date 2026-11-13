package com.medico.web;

import com.medico.service.*;
import com.medico.service.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/doctor")
public class DoctorPanelController {
    private final DoctorPanelService panel; private final AppointmentService appts;
    public DoctorPanelController(DoctorPanelService panel, AppointmentService appts) { this.panel = panel; this.appts = appts; }

    @GetMapping("/me") public DoctorDto me(@AuthenticationPrincipal Long uid) { return panel.me(uid); }
    @PutMapping("/me") public DoctorDto update(@AuthenticationPrincipal Long uid, @Valid @RequestBody DoctorProfileReq r) { return panel.update(uid, r); }
    @GetMapping("/appointments") public List<DoctorApptDto> list(@AuthenticationPrincipal Long uid) { return appts.doctorAppointments(uid); }
    @PostMapping("/appointments/{id}/complete") public DoctorApptDto complete(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return appts.complete(uid, id); }
    @PostMapping("/appointments/{id}/cancel") public DoctorApptDto cancel(@AuthenticationPrincipal Long uid, @PathVariable Long id) { return appts.cancelByDoctor(uid, id); }
}
