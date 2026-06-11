package com.example.flight.ai;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AdviceController {
    private final AdviceService adviceService;

    public AdviceController(AdviceService adviceService) {
        this.adviceService = adviceService;
    }

    @PostMapping("/advice")
    public AdviceResponse advice(@RequestBody AdviceRequest request) {
        return adviceService.generate(request);
    }
}

