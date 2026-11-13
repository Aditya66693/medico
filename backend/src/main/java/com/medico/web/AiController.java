package com.medico.web;

import com.medico.service.Dtos.*;
import com.medico.service.TriageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/ai")
public class AiController {
    private final TriageService triage;
    public AiController(TriageService triage) { this.triage = triage; }

    @PostMapping("/triage")
    public TriageRes triage(@Valid @RequestBody TriageReq r) { return triage.triage(r.problem()); }
}
