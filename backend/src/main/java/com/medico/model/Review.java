package com.medico.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Getter @Setter @NoArgsConstructor
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Doctor doctor;
    @ManyToOne(optional = false) private User patient;
    @Column(unique = true) private Long appointmentId;
    private int rating;
    @Column(length = 1000) private String comment;
    private Instant createdAt;
}
