package com.medico.model;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String name;
    @Column(unique = true, nullable = false) private String email;
    @Column(nullable = false) private String password;
    private String phone;
    private String gender;
    private java.time.LocalDate dateOfBirth;
    private String bloodGroup;
    private String city;
    @Enumerated(EnumType.STRING) private Role role = Role.PATIENT;

    public Role getRole() { return role == null ? Role.PATIENT : role; }
}
