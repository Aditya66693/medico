package com.medico.service;

import com.medico.model.User;
import com.medico.repo.UserRepository;
import com.medico.service.Dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.Set;

@Service
public class ProfileService {
    static final Set<String> GENDERS = Set.of("Male", "Female", "Other", "Prefer not to say");
    static final Set<String> BLOOD = Set.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
    private final UserRepository users; private final PasswordEncoder enc;
    public ProfileService(UserRepository users, PasswordEncoder enc) { this.users = users; this.enc = enc; }

    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
    private static String blank(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in again"));
    }

    public ProfileDto get(Long id) { return dto(find(id)); }

    @Transactional
    public ProfileDto update(Long id, ProfileReq r) {
        String gender = blank(r.gender()), blood = blank(r.bloodGroup()), phone = blank(r.phone()), city = blank(r.city());
        if (gender != null && !GENDERS.contains(gender)) throw bad("Invalid gender");
        if (blood != null && !BLOOD.contains(blood)) throw bad("Invalid blood group");
        if (phone != null && !phone.matches("[0-9+\\- ]{7,15}")) throw bad("Invalid phone number");
        if (city != null && city.length() > 80) throw bad("City is too long");
        LocalDate dob = r.dateOfBirth();
        if (dob != null && (dob.isAfter(LocalDate.now(AppointmentService.IST)) || dob.isBefore(LocalDate.of(1900, 1, 1))))
            throw bad("Invalid date of birth");
        User u = find(id);
        u.setName(r.name().trim()); u.setPhone(phone); u.setGender(gender);
        u.setDateOfBirth(dob); u.setBloodGroup(blood); u.setCity(city);
        return dto(users.save(u));
    }

    @Transactional
    public void changePassword(Long id, PasswordReq r) {
        User u = find(id);
        if (!enc.matches(r.currentPassword(), u.getPassword())) throw bad("Current password is incorrect");
        u.setPassword(enc.encode(r.newPassword()));
        users.save(u);
    }

    private static ProfileDto dto(User u) {
        return new ProfileDto(u.getName(), u.getEmail(), u.getPhone(), u.getGender(), u.getDateOfBirth(),
                u.getBloodGroup(), u.getCity(), u.getRole().name());
    }
}
