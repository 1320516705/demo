package com.competition.invoice.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.invoice.common.PageResult;
import com.competition.invoice.common.Result;
import com.competition.invoice.model.dto.OutreachRequestDTO;
import com.competition.invoice.model.vo.RecallListVO;
import com.competition.invoice.service.recall.RecallListService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 召回名单 API
 */
@RestController
@RequestMapping("/api/v1/recall")
@RequiredArgsConstructor
public class RecallListController {

    private final RecallListService recallListService;

    /**
     * 分页查询召回列表
     */
    @GetMapping("/list")
    public Result<PageResult<RecallListVO>> list(
            @RequestParam(defaultValue = "") String dataDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "recallScore") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) String personaTag,
            @RequestParam(required = false) String outreachStatus,
            @RequestParam(required = false) BigDecimal scoreMin,
            @RequestParam(required = false) BigDecimal scoreMax,
            @RequestParam(required = false) String keyword) {

        LocalDate date = dataDate.isEmpty()
                ? LocalDate.now().minusDays(1)
                : LocalDate.parse(dataDate);

        IPage<RecallListVO> result = recallListService.pageList(
                date, page, size, sort, order,
                personaTag, outreachStatus,
                scoreMin, scoreMax, keyword);

        return Result.ok(PageResult.of(result));
    }

    /**
     * 司机详情
     */
    @GetMapping("/{id}")
    public Result<RecallListVO> detail(@PathVariable Long id) {
        return Result.ok(recallListService.getDetail(id));
    }

    /**
     * 单个触达
     */
    @PutMapping("/{id}/outreach")
    public Result<Map<String, Object>> outreach(@PathVariable Long id,
                                                 @RequestBody OutreachRequestDTO req) {
        recallListService.outreach(id, req.getChannel(), req.getRemark());
        return Result.ok(Map.of("id", id, "outreachStatus", "CONTACTED"));
    }

    /**
     * 批量触达
     */
    @PostMapping("/batch-outreach")
    public Result<Map<String, Object>> batchOutreach(@RequestBody OutreachRequestDTO req) {
        int[] result = recallListService.batchOutreach(req.getIds(), req.getChannel(), req.getRemark());
        return Result.ok(Map.of("successCount", result[0], "failCount", result[1]));
    }
}
