package com.competition.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.invoice.entity.RecallKpi;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 召回KPI Mapper
 */
@Mapper
public interface RecallKpiMapper extends BaseMapper<RecallKpi> {

    @Select("SELECT * FROM t_recall_kpi WHERE data_date = #{dataDate} LIMIT 1")
    RecallKpi findByDataDate(@Param("dataDate") String dataDate);
}
