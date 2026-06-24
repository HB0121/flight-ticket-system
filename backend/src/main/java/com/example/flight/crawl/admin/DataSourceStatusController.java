package com.example.flight.crawl.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/data-sources/status")
public class DataSourceStatusController {

    private final DataSourceStatusService dataSourceStatusService;

    public DataSourceStatusController(DataSourceStatusService dataSourceStatusService) {
        this.dataSourceStatusService = dataSourceStatusService;
    }

    @GetMapping
    public List<DataSourceStatusService.DataSourceStatus> list() {
        return dataSourceStatusService.listStatuses();
    }
}
