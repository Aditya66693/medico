package com.medico.service;

import com.medico.model.*;
import com.medico.repo.*;
import com.medico.service.Dtos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.security.SecureRandom;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class AppointmentService {
    static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    static final Duration HOLD = Duration.ofMinutes(10);
    static final List<LocalTime> SLOTS = buildSlots();
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom rnd = new SecureRandom();

    private final AppointmentRepository appts;
    private final UserRepository users;
    private final DoctorRepository doctorRepo;
    private final ReviewRepository reviews;
    private final DoctorService doctors;
    private final PaymentService payments;
    @Value("${medico.dev-mode:false}") private boolean devMode;
    @Value("${medico.demo-payments:false}") private boolean demoPayments;

    public AppointmentService(AppointmentRepository appts, UserRepository users, DoctorRepository doctorRepo,
                              ReviewRepository reviews, DoctorService doctors, PaymentService payments) {
        this.appts = appts; this.users = users; this.doctorRepo = doctorRepo;
        this.reviews = reviews; this.doctors = doctors; this.payments = payments;
    }

    private static List<LocalTime> buildSlots() {
        List<LocalTime> l = new ArrayList<>();
        for (LocalTime t = LocalTime.of(10, 0); t.isBefore(LocalTime.of(17, 30)); t = t.plusMinutes(30))
            if (t.getHour() != 13) l.add(t);   // lunch break 13:00-14:00
        return l;
    }

    private static LocalTime parseSlot(String s) {
        try { return LocalTime.parse(s); } catch (Exception e) { throw err(HttpStatus.BAD_REQUEST, "Invalid slot"); }
    }

    private static ResponseStatusException err(HttpStatus s, String m) { return new ResponseStatusException(s, m); }

    private static boolean started(Appointment a) {
        return LocalDateTime.of(a.getDate(), a.getSlot()).isBefore(LocalDateTime.now(IST));
    }

    private boolean holdsSlot(Appointment a) {
        return a.getStatus() == Status.CONFIRMED || a.getStatus() == Status.COMPLETED
                || (a.getStatus() == Status.PENDING_PAYMENT && a.getCreatedAt().isAfter(Instant.now().minus(HOLD)));
    }

    // ---------------------------------------------------------------- patient side

    public List<SlotDto> slots(Long doctorId, LocalDate date) {
        doctors.get(doctorId);
        LocalDate today = LocalDate.now(IST);
        if (date.isBefore(today) || date.isAfter(today.plusDays(14))) throw err(HttpStatus.BAD_REQUEST, "Choose a date within the next 14 days");
        List<LocalTime> taken = appts.findByDoctorIdAndDate(doctorId, date).stream().filter(this::holdsSlot).map(Appointment::getSlot).toList();
        LocalTime now = LocalTime.now(IST);
        return SLOTS.stream().map(t -> new SlotDto(t.toString(),
                !taken.contains(t) && !(date.equals(today) && t.isBefore(now.plusMinutes(30))))).toList();
    }

    /** synchronized + no @Transactional: each save commits before the lock is released (single-instance safe). */
    public synchronized BookRes book(Long userId, BookReq r) {
        Doctor d = doctors.get(r.doctorId());
        final LocalTime slot = parseSlot(r.slot());
        if (!SLOTS.contains(slot)) throw err(HttpStatus.BAD_REQUEST, "Invalid slot");
        boolean ok = slots(d.getId(), r.date()).stream().anyMatch(s -> s.time().equals(slot.toString()) && s.available());
        if (!ok) throw err(HttpStatus.CONFLICT, "That slot is no longer available");

        Appointment a = new Appointment();
        a.setPatient(users.findById(userId).orElseThrow(() -> err(HttpStatus.UNAUTHORIZED, "Account not found, please log in again")));
        a.setDoctor(d); a.setDate(r.date()); a.setSlot(slot);
        a.setProblem(r.problem()); a.setAmount(d.getFee());
        a.setStatus(Status.PENDING_PAYMENT); a.setPaymentStatus("UNPAID");
        a.setCreatedAt(Instant.now()); a.setTicketCode(newTicket());
        boolean demo = !payments.enabled();
        if (demo && !demoPayments) throw err(HttpStatus.SERVICE_UNAVAILABLE, "Online payments are not configured yet");
        try {
            a.setOrderId(demo ? "demo_" + a.getTicketCode() : payments.createOrder(d.getFee(), a.getTicketCode()));
        } catch (Exception e) { throw err(HttpStatus.BAD_GATEWAY, "Payment gateway unavailable, try again"); }
        a = appts.save(a);
        return new BookRes(dto(a), a.getOrderId(), demo ? null : payments.keyId(), a.getAmount() * 100, demo);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public AppointmentDto verifyPayment(Long userId, Long id, VerifyReq v) {
        Appointment a = owned(userId, id);
        if (a.getStatus() == Status.CONFIRMED) return dto(a);
        if (a.getStatus() != Status.PENDING_PAYMENT) throw err(HttpStatus.CONFLICT, "Appointment is not awaiting payment");
        if (!payments.enabled() || !a.getOrderId().equals(v.orderId()) || !payments.verify(v.orderId(), v.paymentId(), v.signature()))
            throw err(HttpStatus.BAD_REQUEST, "Payment verification failed");
        a.setPaymentId(v.paymentId());
        // hold expired and someone else took the slot -> refund instead of double-booking
        boolean expired = a.getCreatedAt().isBefore(Instant.now().minus(HOLD));
        boolean lost = expired && appts.findByDoctorIdAndDate(a.getDoctor().getId(), a.getDate()).stream()
                .anyMatch(o -> !o.getId().equals(a.getId()) && o.getSlot().equals(a.getSlot()) && o.getStatus() == Status.CONFIRMED);
        if (lost) {
            a.setStatus(Status.CANCELLED); a.setPaymentStatus(payments.refund(v.paymentId()) ? "REFUNDED" : "REFUND_FAILED");
            appts.save(a);
            throw err(HttpStatus.CONFLICT, "Slot was taken while paying. Your payment is being refunded.");
        }
        return confirm(a, v.paymentId());
    }

    @Transactional
    public AppointmentDto demoPay(Long userId, Long id) {
        if (payments.enabled() || !demoPayments) throw err(HttpStatus.FORBIDDEN, "Demo payment is disabled");
        Appointment a = owned(userId, id);
        if (a.getStatus() == Status.CONFIRMED) return dto(a);
        if (a.getStatus() != Status.PENDING_PAYMENT) throw err(HttpStatus.CONFLICT, "Appointment is not awaiting payment");
        a.setStatus(Status.CONFIRMED); a.setPaymentStatus("DEMO_PAID"); a.setPaymentId("demo_pay_" + a.getTicketCode());
        return dto(appts.save(a));
    }

    private AppointmentDto confirm(Appointment a, String paymentId) {
        a.setStatus(Status.CONFIRMED); a.setPaymentStatus("PAID"); a.setPaymentId(paymentId);
        return dto(appts.save(a));
    }

    @Transactional
    public AppointmentDto cancel(Long userId, Long id) { return dto(doCancel(owned(userId, id))); }

    private Appointment doCancel(Appointment a) {
        if (a.getStatus() != Status.CONFIRMED && a.getStatus() != Status.PENDING_PAYMENT)
            throw err(HttpStatus.CONFLICT, "This appointment can't be cancelled");
        if (started(a)) throw err(HttpStatus.BAD_REQUEST, "Past appointments can't be cancelled");
        if ("PAID".equals(a.getPaymentStatus()) || "DEMO_PAID".equals(a.getPaymentStatus())) a.setPaymentStatus(payments.refund(a.getPaymentId()) ? "REFUNDED" : "REFUND_FAILED");
        a.setStatus(Status.CANCELLED);
        return appts.save(a);
    }

    @Transactional
    public AppointmentDto review(Long userId, Long id, ReviewReq r) {
        Appointment a = owned(userId, id);
        if (a.getStatus() != Status.CONFIRMED && a.getStatus() != Status.COMPLETED)
            throw err(HttpStatus.BAD_REQUEST, "Only confirmed appointments can be reviewed");
        boolean visited = a.getStatus() == Status.COMPLETED || started(a);
        if (!visited && !devMode) throw err(HttpStatus.BAD_REQUEST, "You can review after your appointment time");
        if (a.isReviewed()) throw err(HttpStatus.CONFLICT, "Already reviewed");
        Review rv = new Review();
        rv.setDoctor(a.getDoctor()); rv.setPatient(a.getPatient()); rv.setAppointmentId(a.getId());
        rv.setRating(r.rating()); rv.setComment(r.comment()); rv.setCreatedAt(Instant.now());
        reviews.save(rv);
        Doctor d = a.getDoctor();
        d.setRatingSum(d.getRatingSum() + r.rating()); d.setRatingCount(d.getRatingCount() + 1);
        doctorRepo.save(d);
        a.setReviewed(true);
        return dto(appts.save(a));
    }

    public List<AppointmentDto> mine(Long userId) {
        return appts.findByPatientIdOrderByDateDescSlotDesc(userId).stream().map(AppointmentService::dto).toList();
    }

    public AppointmentDto get(Long userId, Long id) { return dto(owned(userId, id)); }

    private Appointment owned(Long userId, Long id) {
        Appointment a = appts.findById(id).orElseThrow(() -> err(HttpStatus.NOT_FOUND, "Appointment not found"));
        if (!a.getPatient().getId().equals(userId)) throw err(HttpStatus.NOT_FOUND, "Appointment not found");
        return a;
    }

    // ---------------------------------------------------------------- doctor side

    private Doctor doctorOf(Long userId) {
        return doctorRepo.findByUserId(userId).orElseThrow(() -> err(HttpStatus.FORBIDDEN, "No doctor profile is linked to this account"));
    }

    private Appointment ofDoctor(Long userId, Long id) {
        Doctor d = doctorOf(userId);
        Appointment a = appts.findById(id).orElseThrow(() -> err(HttpStatus.NOT_FOUND, "Appointment not found"));
        if (!a.getDoctor().getId().equals(d.getId())) throw err(HttpStatus.NOT_FOUND, "Appointment not found");
        return a;
    }

    public List<DoctorApptDto> doctorAppointments(Long doctorUserId) {
        Doctor d = doctorOf(doctorUserId);
        return appts.findByDoctorIdOrderByDateDescSlotDesc(d.getId()).stream()
                .filter(a -> a.getStatus() != Status.PENDING_PAYMENT)
                .map(AppointmentService::docDto).toList();
    }

    @Transactional
    public DoctorApptDto complete(Long doctorUserId, Long id) {
        Appointment a = ofDoctor(doctorUserId, id);
        if (a.getStatus() != Status.CONFIRMED) throw err(HttpStatus.CONFLICT, "Only confirmed appointments can be completed");
        if (!started(a) && !devMode) throw err(HttpStatus.BAD_REQUEST, "This appointment hasn't started yet");
        a.setStatus(Status.COMPLETED);
        return docDto(appts.save(a));
    }

    @Transactional
    public DoctorApptDto cancelByDoctor(Long doctorUserId, Long id) { return docDto(doCancel(ofDoctor(doctorUserId, id))); }

    // ---------------------------------------------------------------- helpers

    private String newTicket() {
        StringBuilder sb = new StringBuilder("MED-");
        for (int i = 0; i < 8; i++) sb.append(ALPHABET.charAt(rnd.nextInt(ALPHABET.length())));
        return sb.toString();
    }

    static AppointmentDto dto(Appointment a) {
        Doctor d = a.getDoctor();
        return new AppointmentDto(a.getId(), a.getTicketCode(), a.getStatus().name(), a.getPaymentStatus(), a.getDate(),
                a.getSlot().toString(), a.getAmount(), a.getProblem(), d.getId(), d.getName(), d.getSpecialization(),
                d.getClinicName(), d.getAddress(), a.isReviewed(), a.getPatient().getName());
    }

    static DoctorApptDto docDto(Appointment a) {
        return new DoctorApptDto(a.getId(), a.getTicketCode(), a.getStatus().name(), a.getDate(), a.getSlot().toString(),
                a.getPatient().getName(), a.getPatient().getPhone(), a.getProblem(), a.getAmount());
    }
}
