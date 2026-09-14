package com.zhihuan.user.api;

import com.zhihuan.user.vo.UserBasicVO;

import java.util.Collection;
import java.util.List;

/**
 * 用户 Dubbo 接口：供其它微服务（product/trade/im 等）跨服务调用。
 * 该接口放置于独立 API 模块 zhihuan-user-api，避免服务间依赖整个业务 jar。
 */
public interface UserDubboService {

    /**
     * 按ID查询用户基础信息（脱敏）
     */
    UserBasicVO getBasicById(Long userId);

    /**
     * 批量查询用户基础信息
     */
    List<UserBasicVO> listBasicByIds(Collection<Long> userIds);

    /**
     * 校验用户是否存在且正常
     */
    boolean isUserValid(Long userId);
}