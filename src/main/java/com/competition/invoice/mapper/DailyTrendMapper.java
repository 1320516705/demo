package com.competition.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.invoice.entity.DailyTrend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 每日趋势 Mapper
 */
@Mapper
public interface DailyTrendMapper extends BaseMapper<DailyTrend> {

    /**
     * 查询近N天趋势
     */
    @Select("SELECT * FROM t_daily_trend WHERE data_date <= #{endDate} " +
            "ORDER BY data_date DESC LIMIT #{days}")
    List<DailyTrend> findRecentTrend(@Param("endDate") String endDate, @Param("days") int days);
}
