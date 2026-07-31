package com.competition.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.invoice.entity.DriverDailySnapshot;
import org.apache.ibatis.annotations.Mapper;

/**
 * 司机每日快照 Mapper
 */
@Mapper
public interface DriverDailySnapshotMapper extends BaseMapper<DriverDailySnapshot> {
}
