package com.medico.repo;

import com.medico.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findBySpecializationIgnoreCase(String specialization);
    java.util.Optional<Doctor> findByUserId(Long userId);
}
