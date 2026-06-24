package com.example.flight.flight;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegacySampleDataCleanupTest {

    @Test
    void removesLegacySampleRowsAcrossRuntimeTables() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), eq("sample"))).thenReturn(1);
        when(jdbcTemplate.update(anyString(), eq("sample"), eq("source=sample%"))).thenReturn(1);

        LegacySampleDataCleanup cleanup = new LegacySampleDataCleanup(jdbcTemplate);

        cleanup.purgeSampleData();

        verify(jdbcTemplate, times(2)).update(anyString(), eq("sample"));
        verify(jdbcTemplate).update(anyString(), eq("sample"), eq("source=sample%"));
    }
}
