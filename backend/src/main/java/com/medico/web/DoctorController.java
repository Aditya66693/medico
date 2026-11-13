package com.medico.web;

import com.medico.service.*;
import com.medico.service.Dtos.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api")
public class DoctorController {
    private final DoctorService doctors; private final AppointmentService appts;
    public DoctorController(DoctorService doctors, AppointmentService appts) { this.doctors = doctors; this.appts = appts; }

    @GetMapping("/health") public Map<String, String> health() { return Map.of("status", "ok"); }

    @GetMapping("/specializations") public List<String> specializations() { return Specializations.ALL; }

    @GetMapping("/doctors")
    public List<DoctorDto> search(@RequestParam(required = false) String specialization,
                                  @RequestParam(required = false) Double lat, @RequestParam(required = false) Double lng,
                                  @RequestParam(defaultValue = "3") int limit) {
        return doctors.search(specialization, lat, lng, Math.min(limit, 20));
    }

    @GetMapping("/doctors/{id}") public DoctorDto get(@PathVariable Long id) { return doctors.getDto(id); }

    @GetMapping("/doctors/{id}/slots")
    public List<SlotDto> slots(@PathVariable Long id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return appts.slots(id, date);
    }
}
