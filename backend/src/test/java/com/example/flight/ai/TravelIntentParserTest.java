package com.example.flight.ai;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;

class TravelIntentParserTest {

    @Test
    void parsesRouteDateBudgetAndTimePreference() {
        ParsedTravelIntent intent = TravelIntentParser.parse("下周五从重庆去北京出差，预算1200元，希望上午到");

        assertThat(intent.fromCity()).isEqualTo("重庆");
        assertThat(intent.toCity()).isEqualTo("北京");
        assertThat(intent.date()).isEqualTo(LocalDate.now().with(TemporalAdjusters.next(java.time.DayOfWeek.FRIDAY)));
        assertThat(intent.budget()).isEqualByComparingTo(new BigDecimal("1200"));
        assertThat(intent.timePreference()).isEqualTo(TimePreference.MORNING);
    }

    @Test
    void extractsDestinationWhenOnlyOneCityIsMentioned() {
        ParsedTravelIntent intent = TravelIntentParser.parse("下周五去北京出差，预算1200元，希望上午到");

        assertThat(intent.fromCity()).isNull();
        assertThat(intent.toCity()).isEqualTo("北京");
        assertThat(intent.timePreference()).isEqualTo(TimePreference.MORNING);
    }
}
