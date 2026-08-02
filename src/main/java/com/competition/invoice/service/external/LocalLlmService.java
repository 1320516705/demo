package com.competition.invoice.service.external;

import com.competition.invoice.entity.DriverDailySnapshot;
import com.competition.invoice.model.enums.OutreachChannel;
import org.springframework.stereotype.Component;

/**
 * 本地话术生成器（不含价格/奖励措辞）
 */
@Component
public class LocalLlmService {

    public LLMClient.LLMStrategyResponse generate(DriverDailySnapshot s) {
        String name = s.getDriverName() != null ? s.getDriverName() : "师傅";
        int activeDays = s.getActiveDays7d() != null ? s.getActiveDays7d() : 0;
        double onlineCount = s.getOnlineCount7d() != null ? s.getOnlineCount7d() : 0;

        String script = String.format(
            "%s，近期本市运力偏紧，您常跑的区域订单等候时间较长，方便时上线看看。",
            name);

        // 渠道：高活跃→App推送，其他→短信
        String channel = activeDays >= 3 ? OutreachChannel.APP_PUSH.name() : OutreachChannel.SMS.name();

        LLMClient.LLMStrategyResponse r = new LLMClient.LLMStrategyResponse();
        r.setStrategyScript(script);
        r.setRecommendedChannel(channel);
        return r;
    }
}
