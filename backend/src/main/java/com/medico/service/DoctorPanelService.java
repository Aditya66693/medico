package com.medico.service;

import com.medico.model.Doctor;
import com.medico.repo.DoctorRepository;
import com.medico.service.Dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DoctorPanelService {
    private final DoctorRepository repo;
    public DoctorPanelService(DoctorRepository repo) { this.repo = repo; }

    private Doctor find(Long userId) {
        return repo.findByUserId(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No doctor profile is linked to this account"));
    }

    public DoctorDto me(Long userId) { return DoctorService.toDto(find(userId), null, null); }

    @Transactional
    public DoctorDto update(Long userId, DoctorProfileReq r) {
        DoctorService.checkGeo(r.latitude(), r.longitude());
        Doctor d = find(userId);
        d.setClinicName(r.clinicName().trim()); d.setAddress(r.address().trim());
        d.setLatitude(r.latitude()); d.setLongitude(r.longitude());
        d.setFee(r.fee()); d.setExperienceYears(r.experienceYears());
        return DoctorService.toDto(repo.save(d), null, null);
    }
}
