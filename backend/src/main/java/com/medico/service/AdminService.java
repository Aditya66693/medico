package com.medico.service;

import com.medico.model.*;
import com.medico.repo.*;
import com.medico.service.Dtos.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class AdminService {
    private final DoctorRepository doctorRepo;
    private final UserRepository users;
    private final AppointmentRepository appts;
    private final PasswordEncoder enc;

    public AdminService(DoctorRepository doctorRepo, UserRepository users, AppointmentRepository appts, PasswordEncoder enc) {
        this.doctorRepo = doctorRepo; this.users = users; this.appts = appts; this.enc = enc;
    }

    private static ResponseStatusException err(HttpStatus s, String m) { return new ResponseStatusException(s, m); }

    public Stats stats() {
        return new Stats(doctorRepo.count(), users.countByRole(Role.PATIENT), appts.count(),
                appts.countByStatus(Status.CONFIRMED), appts.countByStatus(Status.COMPLETED),
                appts.countByStatus(Status.CANCELLED), appts.totalPaid());
    }

    public List<AdminDoctorDto> doctors() {
        return doctorRepo.findAll(Sort.by("name")).stream().map(AdminService::dto).toList();
    }

    public List<AppointmentDto> appointments() {
        return appts.findTop200ByOrderByIdDesc().stream().map(AppointmentService::dto).toList();
    }

    @Transactional
    public AdminDoctorDto create(DoctorReq r) {
        String email = r.email() == null ? "" : r.email().trim().toLowerCase();
        if (email.isEmpty() || r.password() == null || r.password().length() < 6)
            throw err(HttpStatus.BAD_REQUEST, "Login email and a password (min 6 chars) are required for a new doctor");
        Doctor d = new Doctor();
        apply(d, r);
        d.setUser(newDoctorUser(d.getName(), email, r.password()));
        return dto(doctorRepo.save(d));
    }

    @Transactional
    public AdminDoctorDto update(Long id, DoctorReq r) {
        Doctor d = doctorRepo.findById(id).orElseThrow(() -> err(HttpStatus.NOT_FOUND, "Doctor not found"));
        apply(d, r);
        String email = r.email() == null ? "" : r.email().trim().toLowerCase();
        boolean hasPw = r.password() != null && !r.password().isBlank();
        if (hasPw && r.password().length() < 6) throw err(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
        if (d.getUser() == null) {
            if (!email.isEmpty()) {
                if (!hasPw) throw err(HttpStatus.BAD_REQUEST, "A password is required to create this doctor's login");
                d.setUser(newDoctorUser(d.getName(), email, r.password()));
            }
        } else {
            User u = d.getUser();
            if (!email.isEmpty() && !email.equals(u.getEmail())) {
                if (users.existsByEmail(email)) throw err(HttpStatus.CONFLICT, "Email already in use");
                u.setEmail(email);
            }
            if (hasPw) u.setPassword(enc.encode(r.password()));
            u.setName(d.getName());
            users.save(u);
        }
        return dto(doctorRepo.save(d));
    }

    @Transactional
    public void delete(Long id) {
        Doctor d = doctorRepo.findById(id).orElseThrow(() -> err(HttpStatus.NOT_FOUND, "Doctor not found"));
        if (appts.existsByDoctorId(id)) throw err(HttpStatus.CONFLICT, "This doctor has appointments and can't be deleted");
        User u = d.getUser();
        doctorRepo.delete(d);
        if (u != null) users.delete(u);
    }

    private User newDoctorUser(String name, String email, String password) {
        if (users.existsByEmail(email)) throw err(HttpStatus.CONFLICT, "Email already in use");
        User u = new User();
        u.setName(name); u.setEmail(email); u.setPassword(enc.encode(password)); u.setRole(Role.DOCTOR);
        return users.save(u);
    }

    private void apply(Doctor d, DoctorReq r) {
        String spec = Specializations.ALL.stream().filter(s -> s.equalsIgnoreCase(r.specialization().trim())).findFirst()
                .orElseThrow(() -> err(HttpStatus.BAD_REQUEST, "Unknown specialization"));
        DoctorService.checkGeo(r.latitude(), r.longitude());
        d.setName(r.name().trim()); d.setSpecialization(spec); d.setClinicName(r.clinicName().trim());
        d.setAddress(r.address().trim()); d.setLatitude(r.latitude()); d.setLongitude(r.longitude());
        d.setFee(r.fee()); d.setExperienceYears(r.experienceYears());
    }

    private static AdminDoctorDto dto(Doctor d) {
        return new AdminDoctorDto(d.getId(), d.getName(), d.getSpecialization(), d.getClinicName(), d.getAddress(),
                d.getLatitude(), d.getLongitude(), d.getFee(), d.getExperienceYears(), d.getRating(), d.getRatingCount(),
                d.getUser() == null ? null : d.getUser().getEmail());
    }
}
