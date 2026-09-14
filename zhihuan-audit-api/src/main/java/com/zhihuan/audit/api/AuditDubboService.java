package com.zhihuan.audit.api;

import com.zhihuan.audit.api.dto.AuditRequestDTO;
import com.zhihuan.audit.api.dto.AuditResultDTO;

import java.util.List;

/**
 * 内容审核 Dubbo 接口：供其它微服务（product/trade 等）跨服务调用。
 * 该接口放置于独立 API 模块 zhihuan-audit-api，避免服务间依赖整个业务 jar。
 */
public interface AuditDubboService {

    /**
     * 审核内容（规则快路径：Aho-Corasick 违规词匹配）。
     */
    AuditResultDTO auditContent(AuditRequestDTO request);

    /**
     * 查询某内容的历史审核记录
     */
    List<AuditResultDTO> getAuditHistory(Long targetId);

    /**
     * 违规词库变更后重建 Aho-Corasick 自动机
     */
    void rebuildSensitiveWordDict();
}