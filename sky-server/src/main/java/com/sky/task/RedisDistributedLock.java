package com.sky.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

@Component
@Slf4j
public class RedisDistributedLock {

    private final StringRedisTemplate redisTemplate;
    private final String instanceId = UUID.randomUUID().toString(); // 应用实例唯一ID
    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

    public RedisDistributedLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        scheduler.setPoolSize(1);
        scheduler.initialize();
    }

    // 自动续期调度任务
    private ScheduledFuture<?> renewalTask;  // renewal: 续约、续期
    @PostConstruct
    public void checkDI() {
        log.info("redisTemplate 注入是否成功: {}", redisTemplate != null);
    }

    /**
     * 尝试加锁
     */
    public boolean tryLock(String lockKey, Duration expire) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, instanceId, expire);
        if (Boolean.TRUE.equals(success)) {
            // 开启自动续期（每过 expire/3 就续一次）
            startRenewalTask(lockKey, expire);
            return true;
        }
        return false;
    }

    /**
     * 解锁（只有持有锁的实例才能释放）
     */
    public void unlock(String lockKey) {
        String value = redisTemplate.opsForValue().get(lockKey);
        if (instanceId.equals(value)) {
            redisTemplate.delete(lockKey);
        }
        stopRenewalTask();
    }

    /**
     * 开启自动续期
     */
    private void startRenewalTask(String lockKey, Duration expire) {
        stopRenewalTask(); // 避免重复开启
        long renewalInterval = expire.toMillis() / 3;
        renewalTask = scheduler.scheduleAtFixedRate(() -> {
            String value = redisTemplate.opsForValue().get(lockKey);  // 如果能拿到，证明锁还没被释放（删除）
            if (instanceId.equals(value)) {
                redisTemplate.expire(lockKey, expire);  // 重新设置过期时间
            }
        }, renewalInterval);  // 每隔 renewalInterval 时间
    }

    /**
     * 停止自动续期
     */
    private void stopRenewalTask() {
        if (renewalTask != null && !renewalTask.isCancelled()) {
            renewalTask.cancel(true);
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }

    public String getInstanceId() {
        return instanceId;
    }
}

