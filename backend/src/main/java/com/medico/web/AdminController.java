package com.medico.web;

import com.medico.service.AdminService;
import com.medico.service.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/admin")
public class AdminController {
    private final AdminService svc;
    public AdminController(AdminService svc) { this.svc = svc; }

    @GetMapping("/stats") public Stats stats() { return svc.stats(); }
    @GetMapping("/doctors") public List<AdminDoctorDto> doctors() { return svc.doctors(); }
    @PostMapping("/doctors") public AdminDoctorDto create(@Valid @RequestBody DoctorReq r) { return svc.create(r); }
    @PutMapping("/doctors/{id}") public AdminDoctorDto update(@PathVariable Long id, @Valid @RequestBody DoctorReq r) { return svc.update(id, r); }
    @DeleteMapping("/doctors/{id}") public void delete(@PathVariable Long id) { svc.delete(id); }
    @GetMapping("/appointments") public List<AppointmentDto> appointments() { return svc.appointments(); }
}
