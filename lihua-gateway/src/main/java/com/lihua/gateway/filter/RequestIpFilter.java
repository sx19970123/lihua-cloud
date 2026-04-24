package com.lihua.gateway.filter;

import com.lihua.cache.manager.LocalCacheManager;
import com.lihua.cache.manager.RedisCacheManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.core.type.TypeReference;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.lihua.cache.enums.RedisKeyPrefixEnum.SYSTEM_IP_BLACKLIST_REDIS_PREFIX;

/**
 * 请求ip过滤器
 */
@Component
@Slf4j
public class RequestIpFilter implements GlobalFilter {

    @Resource
    private RedisCacheManager redisCacheManager;

    @Resource
    private LocalCacheManager localCacheManager;

    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ipMatch(exchange.getRequest());
        return chain.filter(exchange);
    }

    // ip 匹配
    private void ipMatch(ServerHttpRequest request) {
        String key = SYSTEM_IP_BLACKLIST_REDIS_PREFIX.getValue();
        List<String> prohibitIpList = localCacheManager.getWithFallback(key, new TypeReference<>(){}, () -> redisCacheManager.getCacheList(SYSTEM_IP_BLACKLIST_REDIS_PREFIX.getValue(), String.class));
        String currentIp = getIpAddress(request);
        if (!prohibitIpList.isEmpty()) {
            prohibitIpList.forEach(ip -> {

                String regex = ip
                        .replace(".", "\\.")
                        .replace("*", ".*")
                        .replace("?", ".");

                regex = "^" + regex + "$";
                Pattern compiledPattern = Pattern.compile(regex);
                Matcher matcher = compiledPattern.matcher(currentIp);

                if (matcher.matches()) {
                    log.error("");
                }
            });
        }
    }

    /**
     * 根据请求获取ip
     */
    private String getIpAddress(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("X-Real-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            InetSocketAddress remoteAddress = request.getRemoteAddress();
            if (remoteAddress != null) {
                ip = remoteAddress.getAddress().getHostAddress();
            }
        }
        return ip;
    }
}
