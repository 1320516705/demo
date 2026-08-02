package com.competition.invoice.service.kpi;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.invoice.entity.DriverDailySnapshot;
import com.competition.invoice.mapper.DriverDailySnapshotMapper;
import com.competition.invoice.model.vo.KpiCardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KpiService {

    private final DriverDailySnapshotMapper snapshotMapper;

    public KpiCardVO getSummary(LocalDate dataDate) {
        List<DriverDailySnapshot> list = snapshotMapper.selectList(
                new LambdaQueryWrapper<DriverDailySnapshot>()
                        .eq(DriverDailySnapshot::getSnapshotDate, dataDate));

        KpiCardVO vo = new KpiCardVO();
        if (list.isEmpty()) {
            vo.setRecallableCount(0);
            vo.setOnlineYesterdayNotToday(0);
            vo.setExpectedSuccessRate(BigDecimal.ZERO);
            return vo;
        }

        vo.setRecallableCount(list.size());

        // 近7天在线但今天不在线
        long onlineNotToday = list.stream()
                .filter(s -> s.getOnlineCount7d() != null && s.getOnlineCount7d() > 0)
                .filter(s -> s.getLastOrderTime() == null
                        || s.getLastOrderTime().toLocalDate().isBefore(dataDate))
                .count();
        vo.setOnlineYesterdayNotToday((int) onlineNotToday);

        // 默认预期成功率
        vo.setExpectedSuccessRate(BigDecimal.valueOf(0.38));

        return vo;
    }
}
