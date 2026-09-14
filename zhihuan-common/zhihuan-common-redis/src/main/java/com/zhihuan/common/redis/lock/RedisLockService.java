package com.zhihuan.common.redis.lock;

import com.zhihuan.common.exception.BizException;
import com.zhihuan.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁服务（基于 Redisson）。
 * 使用示例：
 * <pre>
 * boolean ok = lockService.tryLock("order:create:" + userId, 3, 10);
 * ...
 * lockService.unlock("order:create:" + userId);
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class RedisLockService {

    private final RedissonClient redissonClient;

    /**
     * 尝试获取锁
     *
     * @param key          锁 Key
     * @param waitSeconds  等待时间（秒）
     * @param leaseSeconds 自动释放时间（秒）
     */
    public boolean tryLock(String key, long waitSeconds, long leaseSeconds) {
        RLock lock = redissonClient.getLock(key);
        try {
            return lock.tryLock(waitSeconds, leaseSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放锁（仅当前线程持有才释放）
     */
    public void unlock(String key) {
        RLock lock = redissonClient.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 加锁执行，获取锁失败抛出业务异常
     */
    public <T> T executeWithLock(String key, long waitSeconds,
                                 long leaseSeconds, Supplier<T> action) {
        if (!tryLock(key, waitSeconds, leaseSeconds)) {
            throw new BizException(ResultCode.REQUEST_LIMIT);
        }
        try {
            return action.get();
        } finally {
            unlock(key);
        }
    }
}
