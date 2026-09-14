package com.zhihuan.audit.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhihuan.audit.api.AuditDubboService;
import com.zhihuan.audit.api.dto.AuditRequestDTO;
import com.zhihuan.audit.api.dto.AuditResultDTO;
import com.zhihuan.audit.entity.AuditRecord;
import com.zhihuan.audit.entity.ViolationWord;
import com.zhihuan.audit.mapper.AuditRecordMapper;
import com.zhihuan.audit.mapper.ViolationWordMapper;
import com.zhihuan.audit.matcher.AhoCorasickMatcher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 内容审核服务实现（规则快路径：Aho-Corasick 违规词匹配 + 审核记录落库）。
 * AI 双引擎为后续增强，本最小版先实现规则快路径。
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditDubboService {

    private final AhoCorasickMatcher matcher;
    private final ViolationWordMapper violationWordMapper;
    private final AuditRecordMapper auditRecordMapper;

    /** 规则引擎渠道 */
    private static final int CHANNEL_RULE = 1;

    /**
     * 启动时从启用违规词加载构建自动机
     */
    @PostConstruct
    public void loadDictionary() {
        rebuildSensitiveWordDict();
    }

    @Override
    public AuditResultDTO auditContent(AuditRequestDTO request) {
        long start = System.currentTimeMillis();
        List<String> hits = matcher.match(request.getContent());

        AuditResultDTO result = new AuditResultDTO();
        if (!hits.isEmpty()) {
            result.setResult(AuditResultDTO.RESULT_REJECT);
            result.setHitWords(hits.stream().distinct().collect(Collectors.joining(",")));
            result.setMessage("命中违规词：" + result.getHitWords());
            // 命中统计 +1
            increaseHitCount(hits);
        } else {
            result.setResult(AuditResultDTO.RESULT_PASS);
            result.setMessage("审核通过");
        }
        result.setDurationMs(System.currentTimeMillis() - start);

        saveAuditRecord(request, result);
        return result;
    }

    @Override
    public List<AuditResultDTO> getAuditHistory(Long targetId) {
        List<AuditRecord> records = auditRecordMapper.selectList(
            new LambdaQueryWrapper<AuditRecord>()
                .eq(AuditRecord::getTargetId, targetId)
                .orderByDesc(AuditRecord::getCreateTime));
        return records.stream().map(this::toResultDTO).collect(Collectors.toList());
    }

    @Override
    public void rebuildSensitiveWordDict() {
        List<String> words = violationWordMapper.selectList(
                new LambdaQueryWrapper<ViolationWord>()
                    .eq(ViolationWord::getStatus, 1))
            .stream()
            .map(ViolationWord::getWord)
            .collect(Collectors.toList());
        matcher.rebuild(words);
    }

    private void saveAuditRecord(AuditRequestDTO request, AuditResultDTO result) {
        AuditRecord record = new AuditRecord();
        record.setId(IdUtil.getSnowflakeNextId());
        record.setTargetId(request.getTargetId());
        record.setTargetType(request.getTargetType());
        record.setContent(request.getContent());
        record.setAuditChannel(CHANNEL_RULE);
        record.setResult(result.getResult());
        record.setHitWords(StringUtils.hasText(result.getHitWords())
            ? JSONUtil.toJsonStr(result.getHitWords().split(",")) : null);
        record.setDurationMs(result.getDurationMs() == null ? 0 : result.getDurationMs().intValue());
        auditRecordMapper.insert(record);
    }

    private void increaseHitCount(List<String> hits) {
        List<String> distinct = hits.stream().distinct().collect(Collectors.toList());
        for (String word : distinct) {
            List<ViolationWord> words = violationWordMapper.selectList(
                new LambdaQueryWrapper<ViolationWord>().eq(ViolationWord::getWord, word));
            for (ViolationWord vw : words) {
                vw.setHitCount(vw.getHitCount() == null ? 1L : vw.getHitCount() + 1);
                violationWordMapper.updateById(vw);
            }
        }
    }

    private AuditResultDTO toResultDTO(AuditRecord record) {
        AuditResultDTO dto = new AuditResultDTO();
        dto.setResult(record.getResult());
        dto.setHitWords(record.getHitWords());
        dto.setMessage(record.getAiReason());
        dto.setDurationMs(record.getDurationMs() == null ? 0L : record.getDurationMs().longValue());
        return dto;
    }
}