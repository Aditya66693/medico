package com.medico.service;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class Dtos {
    private Dtos() {}

    public record RegisterReq(@NotBlank String name, @Email @NotBlank String email,
                              @Size(min = 6, message = "Password must be at least 6 characters") String password, String phone) {}
    public record LoginReq(@NotBlank String email, @NotBlank String password) {}
    public record AuthRes(String token, Long id, String name, String email, String role) {}

    public record DoctorDto(Long id, String name, String specialization, String clinicName, String address,
                            double latitude, double longitude, int fee, int experienceYears,
                            double rating, int ratingCount, Double distanceKm) {}
    public record SlotDto(String time, boolean available) {}

    public record TriageReq(@NotBlank @Size(max = 1000) String problem) {}
    public record TriageRes(String specialization, String reason, String urgency, String source, String disclaimer) {}

    public record BookReq(@NotNull Long doctorId, @NotNull LocalDate date, @NotBlank String slot, String problem) {}
    public record VerifyReq(@NotBlank String orderId, @NotBlank String paymentId, @NotBlank String signature) {}
    public record ReviewReq(@Min(1) @Max(5) int rating, @Size(max = 1000) String comment) {}

    public record AppointmentDto(Long id, String ticketCode, String status, String paymentStatus, LocalDate date,
                                 String slot, int amount, String problem, Long doctorId, String doctorName,
                                 String specialization, String clinicName, String address, boolean reviewed,
                                 String patientName) {}
    public record DoctorReq(@NotBlank String name, @NotBlank String specialization, @NotBlank String clinicName,
                            @NotBlank String address, double latitude, double longitude, @Min(0) int fee,
                            @Min(0) int experienceYears, String email, String password) {}
    public record AdminDoctorDto(Long id, String name, String specialization, String clinicName, String address,
                                 double latitude, double longitude, int fee, int experienceYears,
                                 double rating, int ratingCount, String email) {}
    public record DoctorProfileReq(@NotBlank String clinicName, @NotBlank String address, double latitude,
                                   double longitude, @Min(0) int fee, @Min(0) int experienceYears) {}
    public record DoctorApptDto(Long id, String ticketCode, String status, LocalDate date, String slot,
                                String patientName, String patientPhone, String problem, int amount) {}
    public record Stats(long doctors, long patients, long appointments, long confirmed, long completed,
                        long cancelled, long revenue) {}
    public record ProfileDto(String name, String email, String phone, String gender, LocalDate dateOfBirth,
                             String bloodGroup, String city, String role) {}
    public record ProfileReq(@NotBlank String name, String phone, String gender, LocalDate dateOfBirth,
                             String bloodGroup, String city) {}
    public record PasswordReq(@NotBlank String currentPassword,
                              @Size(min = 6, message = "New password must be at least 6 characters") String newPassword) {}
    public record ConfigDto(boolean paymentsLive, boolean demoPayments) {}
    public record BookRes(AppointmentDto appointment, String orderId, String keyId, int amountPaise, boolean demo) {}
}
