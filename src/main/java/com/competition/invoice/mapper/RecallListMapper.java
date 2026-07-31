package com.competition.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.invoice.entity.RecallList;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 召回名单 Mapper
 */
@Mapper
public interface RecallListMapper extends BaseMapper<RecallList> {

    /**
     * 查询热力图数据：按H3网格聚合
     */
    @Select("SELECT " +
            "  dsd.h3_index as h3Index, " +
            "  COUNT(*) as driverCount, " +
            "  AVG(rl.recall_score) as avgScore " +
            "FROM t_recall_list rl " +
            "JOIN t_driver_daily_snapshot dsd ON rl.driver_id = dsd.driver_id AND rl.data_date = dsd.snapshot_date " +
            "WHERE rl.data_date = #{dataDate} " +
            "GROUP BY dsd.h3_index")
    List<Map<String, Object>> getHeatmapData(@Param("dataDate") String dataDate);

    /**
     * 按人设标签统计数量
     */
    @Select("SELECT persona_tag as tag, COUNT(*) as cnt FROM t_recall_list " +
            "WHERE data_date = #{dataDate} AND persona_tag IS NOT NULL " +
            "GROUP BY persona_tag")
    List<Map<String, Object>> countByPersonaTag(@Param("dataDate") String dataDate);

    /**
     * 按触达状态统计数量
     */
    @Select("SELECT outreach_status as status, COUNT(*) as cnt FROM t_recall_list " +
            "WHERE data_date = #{dataDate} GROUP BY outreach_status")
    List<Map<String, Object>> countByOutreachStatus(@Param("dataDate") String dataDate);

    /**
     * 按分数字段统计（10分段）
     */
    @Select("SELECT FLOOR(recall_score / 10) * 10 as scoreRange, COUNT(*) as cnt " +
            "FROM t_recall_list WHERE data_date = #{dataDate} GROUP BY scoreRange ORDER BY scoreRange")
    List<Map<String, Object>> scoreDistribution(@Param("dataDate") String dataDate);
}
