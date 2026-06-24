package com.example.flight.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PriceContextSeedService {

    private static final Logger log = LoggerFactory.getLogger(PriceContextSeedService.class);

    private final PriceContextRepository priceContextRepository;

    public PriceContextSeedService(PriceContextRepository priceContextRepository) {
        this.priceContextRepository = priceContextRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedIfEmpty() {
        if (priceContextRepository.count() > 0) {
            log.debug("价格上下文已有数据，跳过种子数据初始化");
            return;
        }
        log.info("初始化价格上下文种子数据...");
        seed("上海", "北京",
                "上海-北京航线：工作日早班（8:00-10:00起飞）价格通常高于午班（11:00-14:00）。"
                        + "提前7-14天购票价格最低，当日购票溢价30%-80%。"
                        + "周五和周日下午为价格高峰。",
                "PRICE_TREND");
        seed("上海", "北京",
                "上海-北京航线历史数据显示：经济舱票价区间为700-1800元。"
                        + "低于800元为低价区间，建议立即购买；"
                        + "800-1200元为正常价格；超过1500元为高价，建议观望。",
                "PRICE_RANGE");
        seed("北京", "上海",
                "北京-上海航线：京沪快线航班密集，日均超过40班。"
                        + "早晚班（7:00-9:00、17:00-20:00）价格较高，中午班次价格通常低15%-25%。"
                        + "周二上午通常为价格最低点。",
                "PRICE_TREND");
        seed("北京", "广州",
                "北京-广州航线：周末价格上浮15%-30%，建议周二至周四购买。"
                        + "提前10-20天购票最为划算。暑假期间（6月底-8月）价格上涨明显。",
                "PRICE_TREND");
        seed("广州", "北京",
                "广州-北京航线：反向航线（广州至北京）价格通常低于北京至广州方向约5%-10%。"
                        + "广交会期间（4月中旬和10月中旬）票价大幅上涨。",
                "PRICE_TREND");
        seed("", "",
                "通用规律：节假日前后3天热门航线价格涨幅可达50%-200%。"
                        + "春节、国庆长假前7天即进入价格上升通道。"
                        + "机票价格呈现'越早越便宜'的总体趋势，但起飞前3-7天可能出现临时降价清仓。"
                        + "经济舱价格低于500元通常为特价票，建议立即购买；超过2000元为高价票。",
                "GENERAL_RULE");
        log.info("价格上下文种子数据初始化完成");
    }

    private void seed(String fromCity, String toCity, String text, String type) {
        priceContextRepository.saveContext(fromCity, toCity, null, text, type);
    }
}
