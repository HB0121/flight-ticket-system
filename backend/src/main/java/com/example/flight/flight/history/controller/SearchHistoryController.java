package com.example.flight.flight.history.controller;

import com.example.flight.auth.model.User;
import com.example.flight.flight.history.model.SearchHistoryRecord;
import com.example.flight.flight.history.service.SearchHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me/search-history")
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    public SearchHistoryController(SearchHistoryService searchHistoryService) {
        this.searchHistoryService = searchHistoryService;
    }

    @GetMapping
    public List<SearchHistoryRecord> list(@RequestAttribute("user") User user) {
        return searchHistoryService.list(user.id());
    }
}
