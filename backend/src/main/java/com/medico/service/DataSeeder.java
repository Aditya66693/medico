package com.medico.service;

import com.medico.model.*;
import com.medico.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** SAMPLE DATA ONLY (fictional doctors around Lucknow). Replace with real data / admin panel before real use. */
@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final DoctorRepository repo;
    private final UserRepository users;
    private final PasswordEncoder enc;
    @Value("${medico.dev-mode:false}") private boolean devMode;
    @Value("${medico.seed-sample:true}") private boolean seedSample;
    @Value("${medico.admin-email:}") private String adminEmail;
    @Value("${medico.admin-password:}") private String adminPassword;

    public DataSeeder(DoctorRepository repo, UserRepository users, PasswordEncoder enc) {
        this.repo = repo; this.users = users; this.enc = enc;
    }

    @Override
    public void run(String... args) {
        users.backfillRoles();
        if (seedSample) seedDoctors();
        seedAdmin();
        if (devMode) seedDemoDoctorLogin();
    }

    private void seedAdmin() {
        String email = adminEmail, pw = adminPassword;
        if (email.isBlank() && devMode) {
            email = "admin@medico.local"; pw = "admin123";
            log.warn("DEV MODE: default admin {} / {} created. Set ADMIN_EMAIL and ADMIN_PASSWORD for real use.", email, pw);
        }
        if (email.isBlank() || pw.isBlank() || users.existsByEmail(email.toLowerCase())) return;
        User u = new User();
        u.setName("Admin"); u.setEmail(email.toLowerCase()); u.setPassword(enc.encode(pw)); u.setRole(Role.ADMIN);
        users.save(u);
    }

    private void seedDemoDoctorLogin() {
        String email = "doctor@medico.local";
        if (users.existsByEmail(email)) return;
        repo.findAll().stream().filter(d -> d.getUser() == null && d.getName().equals("Dr. Anil Verma")).findFirst().ifPresent(d -> {
            User u = new User();
            u.setName(d.getName()); u.setEmail(email); u.setPassword(enc.encode("doctor123")); u.setRole(Role.DOCTOR);
            d.setUser(users.save(u));
            repo.save(d);
            log.warn("DEV MODE: demo doctor login {} / doctor123 linked to {}", email, d.getName());
        });
    }

    private void seedDoctors() {
        if (repo.count() > 0) return;
        // name, specialization, clinic, address, lat, lng, fee, exp, ratingSum, ratingCount
        Object[][] rows = {
            {"Dr. Anil Verma", "Neurologist", "NeuroCare Clinic", "Gomti Nagar, Lucknow", 26.8553, 81.0000, 800, 14, 211, 47},
            {"Dr. Shalini Rao", "Neurologist", "Brain & Spine Centre", "Hazratganj, Lucknow", 26.8500, 80.9462, 900, 18, 188, 40},
            {"Dr. Imran Qureshi", "Neurologist", "Mind Health Clinic", "Alambagh, Lucknow", 26.8100, 80.9000, 700, 9, 96, 22},
            {"Dr. Priya Singh", "Dentist", "SmileCraft Dental", "Aliganj, Lucknow", 26.8900, 80.9400, 400, 8, 240, 52},
            {"Dr. Rohit Mishra", "Dentist", "Pearl Dental Studio", "Gomti Nagar, Lucknow", 26.8580, 80.9950, 500, 11, 170, 38},
            {"Dr. Neha Kapoor", "Dentist", "Dental Hub", "Indira Nagar, Lucknow", 26.8800, 81.0100, 350, 6, 82, 19},
            {"Dr. Sandeep Gupta", "Cardiologist", "HeartLine Clinic", "Hazratganj, Lucknow", 26.8520, 80.9480, 1000, 20, 301, 64},
            {"Dr. Meera Joshi", "Cardiologist", "CardioPlus", "Gomti Nagar, Lucknow", 26.8610, 81.0050, 1100, 16, 150, 33},
            {"Dr. Kavita Agarwal", "Dermatologist", "ClearSkin Clinic", "Indira Nagar, Lucknow", 26.8790, 81.0120, 600, 10, 205, 46},
            {"Dr. Arjun Sharma", "Dermatologist", "DermaCare", "Aliganj, Lucknow", 26.8880, 80.9380, 550, 7, 120, 28},
            {"Dr. Vikas Pandey", "Orthopedic", "BoneWell Hospital", "Alambagh, Lucknow", 26.8120, 80.9050, 700, 15, 175, 39},
            {"Dr. Sunita Yadav", "Orthopedic", "JointCare Clinic", "Chinhat, Lucknow", 26.8700, 81.0600, 650, 12, 130, 30},
            {"Dr. Rajesh Tiwari", "General Physician", "City Family Clinic", "Hazratganj, Lucknow", 26.8490, 80.9440, 300, 22, 310, 70},
            {"Dr. Anjali Saxena", "General Physician", "WellCare Clinic", "Gomti Nagar, Lucknow", 26.8540, 80.9980, 350, 9, 180, 41},
            {"Dr. Faisal Khan", "Pediatrician", "Little Steps Clinic", "Aliganj, Lucknow", 26.8910, 80.9420, 500, 13, 222, 48},
            {"Dr. Pooja Mehta", "ENT Specialist", "ENT Care Centre", "Indira Nagar, Lucknow", 26.8810, 81.0090, 500, 12, 140, 31},
            {"Dr. Harsh Vardhan", "Ophthalmologist", "Vision Plus Eye Clinic", "Hazratganj, Lucknow", 26.8510, 80.9500, 600, 17, 160, 35},
            {"Dr. Ritu Bansal", "Gynecologist", "Motherhood Clinic", "Gomti Nagar, Lucknow", 26.8570, 81.0010, 800, 15, 196, 42},
            {"Dr. Karan Malhotra", "Psychiatrist", "Calm Minds Clinic", "Alambagh, Lucknow", 26.8090, 80.9020, 900, 10, 110, 25},
        };
        for (Object[] r : rows) {
            Doctor d = new Doctor();
            d.setName((String) r[0]); d.setSpecialization((String) r[1]); d.setClinicName((String) r[2]);
            d.setAddress((String) r[3]); d.setLatitude((double) r[4]); d.setLongitude((double) r[5]);
            d.setFee((int) r[6]); d.setExperienceYears((int) r[7]); d.setRatingSum((int) r[8]); d.setRatingCount((int) r[9]);
            repo.save(d);
        }
    }
}
