package com.medico.repo;

import com.medico.model.Appointment;
import com.medico.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientIdOrderByDateDescSlotDesc(Long patientId);
    List<Appointment> findByDoctorIdAndDate(Long doctorId, LocalDate date);
    List<Appointment> findByDoctorIdOrderByDateDescSlotDesc(Long doctorId);
    List<Appointment> findTop200ByOrderByIdDesc();
    boolean existsByDoctorId(Long doctorId);
    long countByStatus(Status status);
    @Query("select coalesce(sum(a.amount), 0) from Appointment a where a.paymentStatus = 'PAID'")
    long totalPaid();
}
