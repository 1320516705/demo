package com.competition.invoice.service.recall;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.invoice.common.BizException;
import com.competition.invoice.entity.RecallList;
import com.competition.invoice.mapper.RecallListMapper;
import com.competition.invoice.model.enums.OutreachChannel;
import com.competition.invoice.model.enums.OutreachStatus;
import com.competition.invoice.model.enums.PersonaTag;
import com.competition.invoice.model.vo.RecallListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 召回名单服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecallListService {

    private final RecallListMapper recallListMapper;

    /**
     * 分页查询召回列表（支持多条件筛选 + 排序）
     */
    public IPage<RecallListVO> pageList(LocalDate dataDate, int page, int size,
                                         String sort, String order,
                                         String personaTag, String outreachStatus,
                                         BigDecimal scoreMin, BigDecimal scoreMax,
                                         String keyword) {

        LambdaQueryWrapper<RecallList> qw = new LambdaQueryWrapper<>();
        qw.eq(RecallList::getDataDate, dataDate);

        // 筛选条件
        if (personaTag != null && !personaTag.isEmpty()) {
            qw.eq(RecallList::getPersonaTag, personaTag);
        }
        if (outreachStatus != null && !outreachStatus.isEmpty()) {
            qw.eq(RecallList::getOutreachStatus, outreachStatus);
        }
        if (scoreMin != null) {
            qw.ge(RecallList::getRecallScore, scoreMin);
        }
        if (scoreMax != null) {
            qw.le(RecallList::getRecallScore, scoreMax);
        }
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like(RecallList::getDriverName, keyword)
                    .or().like(RecallList::getDriverId, keyword)
                    .or().like(RecallList::getPhone, keyword));
        }

        // 排序
        boolean isAsc = "asc".equalsIgnoreCase(order);
        if ("recallScore".equals(sort)) {
            qw.orderBy(true, isAsc, RecallList::getRecallScore);
        } else {
            qw.orderByDesc(RecallList::getRecallScore); // 默认按分数降序
        }

        IPage<RecallList> result = recallListMapper.selectPage(
                new Page<>(page, size), qw);

        return result.convert(this::toVO);
    }

    /**
     * 查询司机详情
     */
    public RecallListVO getDetail(Long id) {
        RecallList rl = recallListMapper.selectById(id);
        if (rl == null) {
            throw new BizException(404, "司机召回记录不存在");
        }
        RecallListVO vo = toVO(rl);
        vo.setLlmResponseRaw(rl.getLlmResponseRaw()); // 详情返回 LLM 原始响应
        return vo;
    }

    /**
     * 单个触达
     */
    public void outreach(Long id, String channel, String remark) {
        RecallList rl = recallListMapper.selectById(id);
        if (rl == null) {
            throw new BizException(404, "司机召回记录不存在");
        }
        if (!"PENDING".equals(rl.getOutreachStatus())) {
            throw new BizException("该司机已触达，无需重复操作");
        }

        rl.setOutreachStatus(OutreachStatus.CONTACTED.name());
        rl.setOutreachChannel(channel != null ? channel : "SMS");
        rl.setOutreachTime(LocalDateTime.now());
        rl.setOutreachRemark(remark);
        recallListMapper.updateById(rl);

        log.info("触达成功: driverId={}, channel={}", rl.getDriverId(), channel);
    }

    /**
     * 批量触达
     */
    public int[] batchOutreach(java.util.List<Long> ids, String channel, String operatorId) {
        int success = 0, fail = 0;
        for (Long id : ids) {
            try {
                RecallList rl = recallListMapper.selectById(id);
                if (rl != null && "PENDING".equals(rl.getOutreachStatus())) {
                    rl.setOutreachStatus(OutreachStatus.CONTACTED.name());
                    rl.setOutreachChannel(channel != null ? channel : "SMS");
                    rl.setOutreachTime(LocalDateTime.now());
                    rl.setOperatorId(operatorId);
                    recallListMapper.updateById(rl);
                    success++;
                } else {
                    fail++;
                }
            } catch (Exception e) {
                log.error("批量触达失败, id={}", id, e);
                fail++;
            }
        }
        log.info("批量触达完成: success={}, fail={}", success, fail);
        return new int[]{success, fail};
    }

    private RecallListVO toVO(RecallList rl) {
        RecallListVO vo = new RecallListVO();
        vo.setId(rl.getId());
        vo.setDriverId(rl.getDriverId());
        vo.setDriverName(rl.getDriverName());
        vo.setPhone(rl.getPhone());
        vo.setRecallScore(rl.getRecallScore());
        vo.setPersonaTag(rl.getPersonaTag());
        vo.setStrategyScript(rl.getStrategyScript());
        vo.setRecommendedChannel(rl.getRecommendedChannel());
        vo.setOutreachStatus(rl.getOutreachStatus());
        vo.setOutreachTime(rl.getOutreachTime());

        // 中文标签
        if (rl.getPersonaTag() != null) {
            try {
                vo.setPersonaTagLabel(PersonaTag.valueOf(rl.getPersonaTag()).getLabel());
            } catch (IllegalArgumentException ignored) {}
        }
        if (rl.getRecommendedChannel() != null) {
            try {
                vo.setRecommendedChannelLabel(OutreachChannel.valueOf(rl.getRecommendedChannel()).getLabel());
            } catch (IllegalArgumentException ignored) {}
        }
        if (rl.getOutreachStatus() != null) {
            try {
                vo.setOutreachStatusLabel(OutreachStatus.valueOf(rl.getOutreachStatus()).getLabel());
            } catch (IllegalArgumentException ignored) {}
        }
        return vo;
    }
}
