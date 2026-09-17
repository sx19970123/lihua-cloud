package com.lihua.client.config;

import com.lihua.common.enums.CustomHttpHeader;
import com.lihua.common.utils.crypt.HmacUtils;
import com.lihua.common.utils.date.DateUtils;
import com.lihua.common.utils.trace.TraceIdUtils;
import com.lihua.security.manager.LoginUserContext;
import com.lihua.web.utils.WebUtils;
import io.netty.channel.ChannelOption;
import jakarta.annotation.Resource;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Resource
    private ClientProperties clientProperties;

    // 内部 RPC 签名密钥（来自 lihua-common.yaml rpc 段；无默认值=缺失启动失败）
    @Value("${rpc.signKey}")
    private String internalSignKey;

    /**
     * WebClient 统一配置，每个接口配置 WebClientFactoryBean
     */
    @Bean
    public WebClient.Builder webClientBuilder(ReactorLoadBalancerExchangeFilterFunction filterFunction) {
        return WebClient
            .builder()
            .clientConnector(initConnector())
            // 负载均衡过滤器
            .filter(filterFunction)
            // 透传
            .filter((request, next) -> {
                // token透传
                String token = LoginUserContext.getToken();

                //  构建新的 request
                ClientRequest.Builder builder = ClientRequest.from(request);

                if (StringUtils.hasText(token)) {
                    builder.header(CustomHttpHeader.TOKEN.getValue(), token);
                }

                // traceId 透传（MDC 由入口 TraceIdFilter 写入）
                String traceId = MDC.get(TraceIdUtils.MDC_KEY);
                if (StringUtils.hasText(traceId)) {
                    builder.header(CustomHttpHeader.TRACE_ID.getValue(), traceId);
                }

                // 原始请求的 ip 与客户端类型透传（仅请求线程传播；非请求线程无原始上下文可透传）
                if (WebUtils.getCurrentRequest() != null) {
                    String ipAddress = LoginUserContext.getIpAddress();
                    if (StringUtils.hasText(ipAddress)) {
                        builder.header(CustomHttpHeader.IP.getValue(), ipAddress);
                    }
                    String clientType = LoginUserContext.getClientType();
                    if (StringUtils.hasText(clientType)) {
                        builder.header(CustomHttpHeader.CLIENT_TYPE.getValue(), clientType);
                    }
                }

                // 签名
                long timeMillis = DateUtils.nowTimeStamp();
                String sign = HmacUtils.hmacSha256(
                        internalSignKey,
                        String.format("%s:%s:%s",
                                request.method().name(),
                                request.url().getPath(),
                                timeMillis)
                );

                builder.header(CustomHttpHeader.SIGN.getValue(), sign);
                builder.header(CustomHttpHeader.TIMESTAMP.getValue(), String.valueOf(timeMillis));

                return next.exchange(builder.build());
            });
    }

    /**
     * 配置连接/超时时间（connector 启动期一次性构建，配置变更需重启生效）
     */
    private ReactorClientHttpConnector initConnector() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(clientProperties.getConnectTimeout().toMillis()))
                .responseTimeout(clientProperties.getResponseTimeout());
        return new ReactorClientHttpConnector(httpClient);
    }
}
