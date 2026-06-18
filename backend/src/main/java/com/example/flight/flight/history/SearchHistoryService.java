package com.example.flight.flight.history;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;

    public SearchHistoryService(SearchHistoryRepository searchHistoryRepository) {
        this.searchHistoryRepository = searchHistoryRepository;
    }

    public SearchHistoryRecord record(Long userId, String fromCity, String toCity, LocalDate travelDate, String dataSource) {
        return searchHistoryRepository.append(userId, new SearchHistoryCommand(fromCity, toCity, travelDate, dataSource));
    }

    public List<SearchHistoryRecord> list(Long userId) {
        return searchHistoryRepository.findByUserId(userId);
    }
}
