package com.zhihuan.trade.service.impl;

import cn.hutool.core.util.IdUtil;
import com.zhihuan.common.exception.BizException;
import com.zhihuan.trade.common.FlowType;
import com.zhihuan.trade.entity.FundFlow;
import com.zhihuan.trade.entity.UserAccount;
import com.zhihuan.trade.mapper.FundFlowMapper;
import com.zhihuan.trade.mapper.UserAccountMapper;
import com.zhihuan.trade.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 资金账户实现：所有资金变动写 fund_flow，余额用乐观锁(version)保障并发安全。
 * 跨账户一致性由上层 @GlobalTransactional(Seata AT) 保证。
 */
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserAccountMapper accountMapper;
    private final FundFlowMapper fundFlowMapper;

    @Override
    public void ensureAccount(Long userId) {
        if (userId == null) {
            return;
        }
        UserAccount acc = accountMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUserId, userId));
        if (acc == null) {
            acc = new UserAccount();
            acc.setUserId(userId);
            acc.setBalance(BigDecimal.ZERO);
            acc.setFrozenBalance(BigDecimal.ZERO);
            acc.setTotalIncome(BigDecimal.ZERO);
            acc.setTotalExpense(BigDecimal.ZERO);
            accountMapper.insert(acc);
        }
    }

    @Override
    public void deposit(Long userId, BigDecimal amount) {
        ensureAccount(userId);
        UserAccount acc = getByUserId(userId);
        acc.setBalance(acc.getBalance().add(amount));
        accountMapper.updateById(acc);
        insertFlow(null, userId, FlowType.COLLECT, amount, acc.getBalance(), id());
    }

    @Override
    public void freeze(Long userId, Long orderId, BigDecimal amount) {
        UserAccount acc = getByUserId(userId);
        if (acc.getBalance().compareTo(amount) < 0) {
            throw new BizException("余额不足");
        }
        acc.setBalance(acc.getBalance().subtract(amount));
        acc.setFrozenBalance(acc.getFrozenBalance().add(amount));
        acc.setTotalExpense(acc.getTotalExpense().add(amount));
        accountMapper.updateById(acc);
        insertFlow(orderId, userId, FlowType.FREEZE, amount, acc.getBalance(), id());
    }

    @Override
    public void unfreeze(Long userId, Long orderId, BigDecimal amount) {
        UserAccount acc = getByUserId(userId);
        if (acc.getFrozenBalance().compareTo(amount) < 0) {
            throw new BizException("冻结余额不足");
        }
        acc.setFrozenBalance(acc.getFrozenBalance().subtract(amount));
        acc.setBalance(acc.getBalance().add(amount));
        acc.setTotalExpense(acc.getTotalExpense().subtract(amount));
        accountMapper.updateById(acc);
        insertFlow(orderId, userId, FlowType.UNFREEZE, amount, acc.getBalance(), id());
    }

    @Override
    public void collect(Long buyerId, Long sellerId, Long orderId, BigDecimal amount) {
        // 解冻买家冻结余额（冻结 -> 清空）
        UserAccount buyer = getByUserId(buyerId);
        if (buyer.getFrozenBalance().compareTo(amount) < 0) {
            throw new BizException("冻结余额不足，无法放款");
        }
        buyer.setFrozenBalance(buyer.getFrozenBalance().subtract(amount));
        buyer.setTotalExpense(buyer.getTotalExpense().subtract(amount));
        accountMapper.updateById(buyer);
        insertFlow(orderId, buyerId, FlowType.UNFREEZE, amount, buyer.getBalance(), id());

        // 收款给卖家
        ensureAccount(sellerId);
        UserAccount seller = getByUserId(sellerId);
        seller.setBalance(seller.getBalance().add(amount));
        seller.setTotalIncome(seller.getTotalIncome().add(amount));
        accountMapper.updateById(seller);
        insertFlow(orderId, sellerId, FlowType.COLLECT, amount, seller.getBalance(), id());
    }

    @Override
    public void refund(Long userId, Long orderId, BigDecimal amount) {
        // 从冻结余额退还给买家
        UserAccount acc = getByUserId(userId);
        if (acc.getFrozenBalance().compareTo(amount) < 0) {
            throw new BizException("冻结余额不足，无法退款");
        }
        acc.setFrozenBalance(acc.getFrozenBalance().subtract(amount));
        acc.setBalance(acc.getBalance().add(amount));
        acc.setTotalExpense(acc.getTotalExpense().subtract(amount));
        accountMapper.updateById(acc);
        insertFlow(orderId, userId, FlowType.REFUND, amount, acc.getBalance(), id());
    }

    private UserAccount getByUserId(Long userId) {
        UserAccount acc = accountMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUserId, userId));
        if (acc == null) {
            throw new BizException("账户不存在，请先充值");
        }
        return acc;
    }

    /** 资金流水号 */
    private String id() {
        return "F" + IdUtil.getSnowflakeNextId();
    }

    private void insertFlow(Long orderId, Long userId, int flowType, BigDecimal amount,
                            BigDecimal balanceAfter, String flowNo) {
        FundFlow flow = new FundFlow();
        flow.setId(IdUtil.getSnowflakeNextId());
        flow.setFlowNo(flowNo);
        flow.setOrderId(orderId == null ? 0L : orderId);
        flow.setUserId(userId);
        flow.setFlowType(flowType);
        flow.setAmount(amount);
        flow.setBalanceAfter(balanceAfter);
        flow.setStatus(1);
        fundFlowMapper.insert(flow);
    }
}