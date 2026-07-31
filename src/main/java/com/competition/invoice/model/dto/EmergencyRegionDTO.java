package com.competition.invoice.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 应急圈选区域请求
 */
@Data
public class EmergencyRegionDTO {

    /** 多边形顶点: [[lng, lat], [lng, lat], ...] */
    @NotEmpty(message = "圈选区域不能为空")
    @Size(min = 3, message = "至少需要3个顶点")
    private List<double[]> polygon;

    /** 区域描述 */
    private String regionDesc;

    /** 操作人ID */
    private String operatorId;
}
