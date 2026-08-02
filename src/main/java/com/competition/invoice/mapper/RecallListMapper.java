package com.competition.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.invoice.entity.RecallList;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface RecallListMapper extends BaseMapper<RecallList> {

    @Select("SELECT outreach_status as status, COUNT(*) as cnt FROM t_recall_list " +
            "WHERE data_date = #{dataDate} GROUP BY outreach_status")
    List<Map<String, Object>> countByOutreachStatus(@Param("dataDate") String dataDate);

    @Select("SELECT FLOOR(recall_score / 10) * 10 as scoreRange, COUNT(*) as cnt " +
            "FROM t_recall_list WHERE data_date = #{dataDate} GROUP BY scoreRange ORDER BY scoreRange")
    List<Map<String, Object>> scoreDistribution(@Param("dataDate") String dataDate);
}
