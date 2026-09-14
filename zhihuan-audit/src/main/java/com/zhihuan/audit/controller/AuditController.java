package com.zhihuan.audit.controller;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuan.audit.api.dto.AuditResultDTO;
import com.zhihuan.audit.entity.AuditRecord;
import com.zhihuan.audit.entity.ViolationWord;
import com.zhihuan.audit.mapper.AuditRecordMapper;
import com.zhihuan.audit.mapper.ViolationWordMapper;
import com.zhihuan.audit.service.impl.AuditServiceImpl;
import com.zhihuan.common.exception.BizException;
import com.zhihuan.common.result.Result;
import com.zhihuan.common.result.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

/**
 * 审核管理接口：违规词库维护 + 审核记录查询
 */
@Tag(name = "审核管理")
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    private final ViolationWordMapper violationWordMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final AuditServiceImpl auditService;

    @Operation(summary = "新增违规词")
    @PostMapping("/word")
    public Result<Long> addWord(@RequestBody ViolationWord word) {
        if (!StringUtils.hasText(word.getWord())) {
            throw new BizException(ResultCode.BIZ_ERROR, "违规词不能为空");
        }
        word.setId(IdUtil.getSnowflakeNextId());
        if (word.getCategory() == null) {
            word.setCategory(0);
        }
        if (word.getLevel() == null) {
            word.setLevel(1);
        }
        if (word.getStatus() == null) {
            word.setStatus(1);
        }
        violationWordMapper.insert(word);
        auditService.rebuildSensitiveWordDict();
        return Result.success(word.getId());
    }

    @Operation(summary = "违规词分页")
    @GetMapping("/word/page")
    public Result<IPage<ViolationWord>> pageWord(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(violationWordMapper.selectPage(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<ViolationWord>().orderByDesc(ViolationWord::getUpdateTime)));
    }

    @Operation(summary = "启用/停用违规词")
    @PutMapping("/word/{id}/status")
    public Result<Void> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        ViolationWord word = new ViolationWord();
        word.setId(id);
        word.setStatus(status);
        violationWordMapper.updateById(word);
        auditService.rebuildSensitiveWordDict();
        return Result.success();
    }

    @Operation(summary = "删除违规词")
    @DeleteMapping("/word/{id}")
    public Result<Void> deleteWord(@RequestParam Long id) {
        violationWordMapper.deleteById(id);
        auditService.rebuildSensitiveWordDict();
        return Result.success();
    }

    @Operation(summary = "审核记录分页")
    @GetMapping("/record/page")
    public Result<IPage<AuditRecord>> pageRecord(
            @RequestParam(required = false) Long targetId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        LambdaQueryWrapper<AuditRecord> wrapper = new LambdaQueryWrapper<>();
        if (targetId != null) {
            wrapper.eq(AuditRecord::getTargetId, targetId);
        }
        wrapper.orderByDesc(AuditRecord::getCreateTime);
        return Result.success(auditRecordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    @Operation(summary = "按目标查询审核历史（Dubbo 校验用）")
    @GetMapping("/record/history")
    public Result<?> history(@RequestParam Long targetId) {
        return Result.success(auditService.getAuditHistory(targetId));
    }
}