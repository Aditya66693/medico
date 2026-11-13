package com.medico.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity @Getter @Setter @NoArgsConstructor
public class Appointment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private User patient;
    @ManyToOne(optional = false) private Doctor doctor;
    private LocalDate date;
    private LocalTime slot;
    @Enumerated(EnumType.STRING) private Status status;
    private String paymentStatus;          // UNPAID, PAID, REFUNDED, REFUND_FAILED
    @Column(length = 1000) private String problem;
    private int amount;                    // INR
    @Column(unique = true) private String ticketCode;
    private String orderId;
    private String paymentId;
    private Instant createdAt;
    private boolean reviewed;
}
