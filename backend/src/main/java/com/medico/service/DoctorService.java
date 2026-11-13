package com.medico.service;

import com.medico.model.Doctor;
import com.medico.repo.DoctorRepository;
import com.medico.service.Dtos.DoctorDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Comparator;
import java.util.List;

@Service
public class DoctorService {
    private final DoctorRepository repo;
    public DoctorService(DoctorRepository repo) { this.repo = repo; }

    /** Nearest-first when lat/lng given (Haversine), otherwise best-rated first. */
    public List<DoctorDto> search(String spec, Double lat, Double lng, int limit) {
        List<Doctor> docs = (spec == null || spec.isBlank()) ? repo.findAll() : repo.findBySpecializationIgnoreCase(spec.trim());
        boolean geo = lat != null && lng != null;
        Comparator<DoctorDto> cmp = geo
                ? Comparator.comparingDouble(DoctorDto::distanceKm).thenComparing(Comparator.comparingDouble(DoctorDto::rating).reversed())
                : Comparator.comparingDouble(DoctorDto::rating).reversed();
        return docs.stream().map(d -> toDto(d, geo ? lat : null, geo ? lng : null)).sorted(cmp).limit(Math.max(1, limit)).toList();
    }

    public Doctor get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
    }

    public DoctorDto getDto(Long id) { return toDto(get(id), null, null); }

    static DoctorDto toDto(Doctor d, Double lat, Double lng) {
        Double dist = (lat == null || lng == null) ? null : Math.round(haversineKm(lat, lng, d.getLatitude(), d.getLongitude()) * 10) / 10.0;
        return new DoctorDto(d.getId(), d.getName(), d.getSpecialization(), d.getClinicName(), d.getAddress(),
                d.getLatitude(), d.getLongitude(), d.getFee(), d.getExperienceYears(), d.getRating(), d.getRatingCount(), dist);
    }

    static void checkGeo(double lat, double lng) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid latitude/longitude");
    }

    static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0, dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }
}
