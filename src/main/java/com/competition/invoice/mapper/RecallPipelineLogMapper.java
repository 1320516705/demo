package com.competition.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.invoice.entity.RecallPipelineLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 管道执行日志 Mapper
 */
@Mapper
public interface RecallPipelineLogMapper extends BaseMapper<RecallPipelineLog> {

    @Select("SELECT * FROM t_recall_pipeline_log WHERE status = 'COMPLETED' ORDER BY id DESC LIMIT 1")
    RecallPipelineLog findLastCompleted();
}
