package com.competition.invoice.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 触达请求 DTO
 */
@Data
public class OutreachRequestDTO {

    /** 单个触达用 */
    private Long id;

    /** 批量触达用 */
    @NotEmpty(message = "司机ID列表不能为空")
    private List<Long> ids;

    /** 触达渠道 */
    private String channel;

    /** 备注 */
    private String remark;
}
