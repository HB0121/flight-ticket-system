package com.example.flight.flight;

import java.util.List;

public interface FlightSearchPort {
    List<Flight> search(FlightSearchCriteria criteria);
}

