package com.medico.model;

import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
public class Doctor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String name;
    private String specialization;
    private String clinicName;
    private String address;
    private double latitude;
    private double longitude;
    private int fee;               // INR
    private int experienceYears;
    private int ratingSum;
    private int ratingCount;
    @OneToOne private User user;   // login account of this doctor (nullable for sample doctors)

    public double getRating() {
        return ratingCount == 0 ? 0 : Math.round(ratingSum * 10.0 / ratingCount) / 10.0;
    }
}
