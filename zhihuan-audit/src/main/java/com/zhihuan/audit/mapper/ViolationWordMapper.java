package com.zhihuan.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhihuan.audit.entity.ViolationWord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 违规词库 Mapper
 */
@Mapper
public interface ViolationWordMapper extends BaseMapper<ViolationWord> {
}