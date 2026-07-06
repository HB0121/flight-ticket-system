package com.example.flight.flight.repository;

import com.example.flight.flight.dto.FlightSearchCriteria;
import com.example.flight.flight.model.Flight;
import java.util.List;

public interface FlightSearchPort {
    List<Flight> search(FlightSearchCriteria criteria);
}
