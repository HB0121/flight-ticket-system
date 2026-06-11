package com.example.flight.ai;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AdviceController {
    private final AdviceService adviceService;
    private final TimingService timingService;

    public AdviceController(AdviceService adviceService, TimingService timingService) {
        this.adviceService = adviceService;
        this.timingService = timingService;
    }

    @PostMapping("/advice")
    public AdviceResponse advice(@RequestBody AdviceRequest request) {
        return adviceService.generate(request);
    }

    @PostMapping("/timing")
    public TimingResponse timing(@RequestBody TimingRequest request) {
        return timingService.analyze(request);
    }
}
