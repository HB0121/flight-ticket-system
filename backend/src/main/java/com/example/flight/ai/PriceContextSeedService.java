package com.example.flight.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 价格上下文种子数据初始化服务。
 *
 * 职责：在应用启动完成后（ApplicationReadyEvent），如果 price_context 表为空，
 * 自动写入预定义的典型航线价格规律知识。
 *
 * 种子数据覆盖的航线：
 * - 上海 → 北京 / 北京 → 上海（京沪快线，高频航线）
 * - 北京 → 广州 / 广州 → 北京（南北干线，广交会影响）
 * - 通用规律（from_city 和 to_city 均为空字符串），适用于所有航线
 *
 * 每条种子数据的 context_type：
 * - PRICE_TREND:  价格趋势规律（高峰期、低价时段、提前购票建议）
 * - PRICE_RANGE:  价格区间参考（低价/正常/高价分界线）
 * - GENERAL_RULE: 通用购票规律（节假日溢价、清仓规律等）
 *
 * 这些种子数据在 RAG 检索中被 {@link PriceContextRepository} 检索，
 * 为 {@link TimingService} 的购票时机分析提供历史规律参考。
 *
 * 设计说明：
 * - 使用 @EventListener(ApplicationReadyEvent.class) 确保在 MySQL 连接就绪后执行
 * - 先检查 count() > 0，避免重复写入种子数据
 */
@Component
public class PriceContextSeedService {

    private static final Logger log = LoggerFactory.getLogger(PriceContextSeedService.class);

    /** 价格上下文仓储 —— 用于写入和检查种子数据 */
    private final PriceContextRepository priceContextRepository;

    public PriceContextSeedService(PriceContextRepository priceContextRepository) {
        this.priceContextRepository = priceContextRepository;
    }

    /**
     * 应用启动完成后自动执行。仅当 price_context 表为空时才写入种子数据。
     *
     * 监听 ApplicationReadyEvent 而非 ContextRefreshedEvent 的原因是：
     * ApplicationReadyEvent 在更晚的阶段触发（所有 CommandLineRunner 和
     * ApplicationRunner 执行完毕之后），此时数据库连接池已完全就绪。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void seedIfEmpty() {
        // 如果已有数据（非首次启动），跳过种子数据初始化
        if (priceContextRepository.count() > 0) {
            log.debug("价格上下文已有数据，跳过种子数据初始化");
            return;
        }
        log.info("初始化价格上下文种子数据...");

        // 上海 → 北京：京沪航线价格趋势规律
        seed("上海", "北京",
                "上海-北京航线：工作日早班（8:00-10:00起飞）价格通常高于午班（11:00-14:00）。"
                        + "提前7-14天购票价格最低，当日购票溢价30%-80%。"
                        + "周五和周日下午为价格高峰。",
                "PRICE_TREND");
        // 上海 → 北京：京沪航线价格区间参考
        seed("上海", "北京",
                "上海-北京航线历史数据显示：经济舱票价区间为700-1800元。"
                        + "低于800元为低价区间，建议立即购买；"
                        + "800-1200元为正常价格；超过1500元为高价，建议观望。",
                "PRICE_RANGE");
        // 北京 → 上海：反向航线价格趋势（京沪快线特点）
        seed("北京", "上海",
                "北京-上海航线：京沪快线航班密集，日均超过40班。"
                        + "早晚班（7:00-9:00、17:00-20:00）价格较高，中午班次价格通常低15%-25%。"
                        + "周二上午通常为价格最低点。",
                "PRICE_TREND");
        // 北京 → 广州：南北干线规律（含广交会影响）
        seed("北京", "广州",
                "北京-广州航线：周末价格上浮15%-30%，建议周二至周四购买。"
                        + "提前10-20天购票最为划算。暑假期间（6月底-8月）价格上涨明显。",
                "PRICE_TREND");
        // 广州 → 北京：反向航线（价格不对称规律）
        seed("广州", "北京",
                "广州-北京航线：反向航线（广州至北京）价格通常低于北京至广州方向约5%-10%。"
                        + "广交会期间（4月中旬和10月中旬）票价大幅上涨。",
                "PRICE_TREND");
        // 通用规律（from_city 和 to_city 均为空字符串，所有航线均可检索到）
        seed("", "",
                "通用规律：节假日前后3天热门航线价格涨幅可达50%-200%。"
                        + "春节、国庆长假前7天即进入价格上升通道。"
                        + "机票价格呈现'越早越便宜'的总体趋势，但起飞前3-7天可能出现临时降价清仓。"
                        + "经济舱价格低于500元通常为特价票，建议立即购买；超过2000元为高价票。",
                "GENERAL_RULE");
        log.info("价格上下文种子数据初始化完成");
    }

    /**
     * 便捷方法：写入单条种子数据。
     *
     * @param fromCity 出发城市（空字符串表示通用规律）
     * @param toCity   目的城市（空字符串表示通用规律）
     * @param text     价格规律文本
     * @param type     规律类型（PRICE_TREND / PRICE_RANGE / GENERAL_RULE）
     */
    private void seed(String fromCity, String toCity, String text, String type) {
        priceContextRepository.saveContext(fromCity, toCity, null, text, type);
    }
}
