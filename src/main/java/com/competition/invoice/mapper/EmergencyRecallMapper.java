package com.competition.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.invoice.entity.EmergencyRecall;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 应急召回 Mapper
 */
@Mapper
public interface EmergencyRecallMapper extends BaseMapper<EmergencyRecall> {

    @Select("SELECT * FROM t_emergency_recall WHERE session_id = #{sessionId} LIMIT 1")
    EmergencyRecall findBySessionId(@Param("sessionId") String sessionId);
}
