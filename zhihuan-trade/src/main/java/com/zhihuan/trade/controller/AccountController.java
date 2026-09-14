package com.zhihuan.trade.controller;

import com.zhihuan.common.result.Result;
import com.zhihuan.trade.entity.UserAccount;
import com.zhihuan.trade.mapper.UserAccountMapper;
import com.zhihuan.trade.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 资金账户接口（充值/查询，便于演示链路；真实场景对接支付）
 */
@Tag(name = "交易-资金账户")
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final UserAccountMapper accountMapper;

    @Operation(summary = "充值")
    @PostMapping("/deposit")
    public Result<Void> deposit(@RequestParam Long userId, @RequestParam BigDecimal amount) {
        accountService.deposit(userId, amount);
        return Result.success();
    }

    @Operation(summary = "账户余额")
    @GetMapping("/balance")
    public Result<UserAccount> balance(@RequestParam Long userId) {
        accountService.ensureAccount(userId);
        return Result.success(accountMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUserId, userId)));
    }
}