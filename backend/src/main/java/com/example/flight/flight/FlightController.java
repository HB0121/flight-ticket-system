package com.example.flight.flight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 航班查询 REST 控制器，暴露 /api/flights 路径下的三个 GET 端点。
 *
 * 职责：接收前端请求参数，委托 FlightRepository 执行查询，返回 JSON 数据。
 * 本 Controller 不包含业务逻辑，保持"薄 Controller"风格。
 *
 * 设计模式：MVC Controller 层，遵循 RESTful 资源导向的 URL 设计。
 */
@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private static final Logger log = LoggerFactory.getLogger(FlightController.class);
    private final FlightRepository flightRepository;

    /** 构造器注入 FlightRepository（单一依赖，无需 @Autowired 注解） */
    public FlightController(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    /**
     * 航班搜索接口：支持出发城市、到达城市、出发日期、数据来源的多条件组合查询。
     * 所有参数均为可选，全为空时返回全部航班。
     *
     * @param fromCity   出发城市（可选，中文城市名如 "上海"）
     * @param toCity     到达城市（可选）
     * @param date       出发日期（可选，ISO 日期格式如 "2026-06-15"）
     * @param dataSource 数据来源过滤（可选，"amadeus" / "sample"）
     * @return 匹配的航班列表（JSON 数组），按价格升序排列
     */
    @GetMapping
    public List<Flight> search(@RequestParam(required = false) String fromCity,
                               @RequestParam(required = false) String toCity,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(required = false) String dataSource) {
        log.debug("航班查询: fromCity={}, toCity={}, date={}, dataSource={}", fromCity, toCity, date, dataSource);
        return flightRepository.search(new FlightSearchCriteria(fromCity, toCity, date, dataSource));
    }

    /**
     * 根据 ID 查询单个航班详情。
     *
     * @param id 航班主键 ID（路径变量，来自 /api/flights/{id}）
     * @return 200 OK 携带航班数据，或 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Flight> findById(@PathVariable Long id) {
        return flightRepository.findById(id)
                .map(ResponseEntity::ok) // 存在则返回 200
                .orElseGet(() -> ResponseEntity.notFound().build()); // 不存在则返回 404
    }

    /**
     * 查询指定航班的价格历史记录。
     * 先校验航班是否存在（不存在返回 404），存在则返回完整价格快照列表。
     *
     * @param id 航班主键 ID（路径变量，来自 /api/flights/{id}/price-history）
     * @return 200 OK 携带按时间升序排列的价格快照列表，或 404 Not Found
     */
    @GetMapping("/{id}/price-history")
    public ResponseEntity<List<FlightPriceSnapshot>> priceHistory(@PathVariable Long id) {
        // 先校验航班存在性，避免查询不存在的航班价格历史
        if (flightRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(flightRepository.findPriceHistory(id));
    }
}
