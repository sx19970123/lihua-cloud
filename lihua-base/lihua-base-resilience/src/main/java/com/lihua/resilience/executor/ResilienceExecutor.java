package com.lihua.resilience.executor;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 通用降级方法
 */
@Component
@Slf4j
public class ResilienceExecutor {

    @Resource
    private CircuitBreakerRegistry cbRegistry;

    @Resource
    private RetryRegistry retryRegistry;


    /**
     * 通用执行方法
     * @param name 降级名称
     * @param supplier 远程调用
     */
    public <T> T execute(String name, Supplier<T> supplier) {
        return execute(name, supplier, false);
    }

    /**
     * 通用执行方法
     * @param name 降级名称
     * @param supplier 远程调用
     * @param useRetry 是否开启重试
     */
    public <T> T execute(String name, Supplier<T> supplier, boolean useRetry) {
        return execute(name, supplier, useRetry, null);
    }

    /**
     * 通用执行方法
     * @param name 降级名称
     * @param supplier 远程调用
     * @param fallback 降级方法
     */
    public <T> T execute(String name, Supplier<T> supplier, Function<Throwable, T> fallback) {
        return execute(name, supplier, false, fallback);
    }

    /**
     * 通用执行方法
     * @param name 降级名称
     * @param supplier 远程调用
     * @param useRetry 是否开启重试
     * @param fallback 降级方法
     */
    public <T> T execute(String name, Supplier<T> supplier, boolean useRetry, Function<Throwable, T> fallback) {
        CircuitBreaker circuitBreaker = cbRegistry.circuitBreaker(name);
        Decorators.DecorateSupplier<T> decorateSupplier = Decorators.ofSupplier(supplier).withCircuitBreaker(circuitBreaker);

        // 重试
        if (useRetry) {
            decorateSupplier.withRetry(retryRegistry.retry(name));
        }

        // 兜底
        if (fallback != null) {
            decorateSupplier.withFallback(ex -> {
                log.error("Resilience 执行失败，触发Fallback，name={}", name, ex);
                return fallback.apply(ex);
            });
        }

        return decorateSupplier.decorate().get();
    }

}
