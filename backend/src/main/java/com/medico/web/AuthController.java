package com.medico.web;

import com.medico.model.User;
import com.medico.repo.UserRepository;
import com.medico.security.JwtService;
import com.medico.service.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users; private final PasswordEncoder enc; private final JwtService jwt;
    public AuthController(UserRepository users, PasswordEncoder enc, JwtService jwt) { this.users = users; this.enc = enc; this.jwt = jwt; }

    @PostMapping("/register")
    public AuthRes register(@Valid @RequestBody RegisterReq r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        User u = new User();
        u.setName(r.name().trim()); u.setEmail(email); u.setPassword(enc.encode(r.password())); u.setPhone(r.phone());
        u = users.save(u);
        return new AuthRes(jwt.create(u.getId(), u.getRole()), u.getId(), u.getName(), u.getEmail(), u.getRole().name());
    }

    @PostMapping("/login")
    public AuthRes login(@Valid @RequestBody LoginReq r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(x -> enc.matches(r.password(), x.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new AuthRes(jwt.create(u.getId(), u.getRole()), u.getId(), u.getName(), u.getEmail(), u.getRole().name());
    }
}
