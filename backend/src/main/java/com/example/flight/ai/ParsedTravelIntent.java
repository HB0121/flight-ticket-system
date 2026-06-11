package com.example.flight.ai;

import java.math.BigDecimal;
import java.time.LocalDate;

record ParsedTravelIntent(String fromCity, String toCity, LocalDate date, BigDecimal budget) {
}
