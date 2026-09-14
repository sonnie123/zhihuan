package com.zhihuan.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhihuan.audit.entity.AuditRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审核记录 Mapper
 */
@Mapper
public interface AuditRecordMapper extends BaseMapper<AuditRecord> {
}