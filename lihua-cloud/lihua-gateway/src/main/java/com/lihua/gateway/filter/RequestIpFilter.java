package com.lihua.gateway.filter;

//import com.lihua.cache.enums.RedisKeyPrefixEnum;
//import com.lihua.cache.manager.LocalCacheManager;
//import com.lihua.cache.manager.RedisCacheManager;
import com.lihua.gateway.exception.GatewayIpIllegalException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.core.type.TypeReference;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 请求ip过滤器
 */
//@Component
@Slf4j
public class RequestIpFilter implements GlobalFilter {

//    @Resource
//    private RedisCacheManager redisCacheManager;
//
//    @Resource
//    private LocalCacheManager localCacheManager;

    // ip匹配缓存
    private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

    @NullMarked
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // return ipMatch(exchange.getRequest(), exchange.getResponse()).switchIfEmpty(chain.filter(exchange));
        return Mono.empty();
    }

    // 匹配ip
//    private Mono<Void> ipMatch(ServerHttpRequest request, ServerHttpResponse response) {
//        // 获取ip黑名单数据
//        String key = RedisKeyPrefixEnum.SYSTEM_IP_BLACKLIST_REDIS_PREFIX.getValue();
//        List<String> prohibitIpList = localCacheManager.getWithFallback(key, new TypeReference<>() {}, () -> redisCacheManager.getCacheList(key, String.class));
//        if (prohibitIpList == null || prohibitIpList.isEmpty()) {
//            return Mono.empty();
//        }
//
//        // 获取当前ip
//        String currentIp = getIpAddress(request);
//        if (currentIp == null) {
//            return Mono.empty();
//        }
//
//        for (String ipRule : prohibitIpList) {
//            // 匹配并缓存黑名单ip
//            Pattern pattern = patternCache.computeIfAbsent(ipRule, rule -> {
//                String regex = rule
//                        .replace(".", "\\.")
//                        .replace("*", ".*")
//                        .replace("?", ".");
//                regex = "^" + regex + "$";
//                return Pattern.compile(regex);
//            });
//            // 匹配到的黑名单ip抛出异常
//            if (pattern.matcher(currentIp).matches() || true) {
//                throw new GatewayIpIllegalException();
//            }
//        }
//
//        return Mono.empty();
//    }

    // 获取当前请求ip
    private String getIpAddress(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            if (ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        }

        ip = request.getHeaders().getFirst("X-Real-IP");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return null;
    }
}