package com.medico.web;

import com.medico.service.Dtos.ConfigDto;
import com.medico.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/config")
public class ConfigController {
    private final PaymentService payments;
    @Value("${medico.demo-payments:false}") private boolean demoPayments;
    public ConfigController(PaymentService payments) { this.payments = payments; }

    @GetMapping public ConfigDto config() { return new ConfigDto(payments.enabled(), !payments.enabled() && demoPayments); }
}
