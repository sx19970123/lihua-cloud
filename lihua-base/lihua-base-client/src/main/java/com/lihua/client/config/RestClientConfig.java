package com.lihua.client.config;

import com.lihua.common.enums.CustomHttpHeader;
import com.lihua.common.utils.crypt.HmacUtils;
import com.lihua.common.utils.date.DateUtils;
import com.lihua.common.utils.trace.TraceIdUtils;
import com.lihua.security.manager.LoginUserContext;
import com.lihua.web.utils.WebUtils;
import jakarta.annotation.Resource;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Resource
    private ClientProperties clientProperties;

    // 内部 RPC 签名密钥（来自 lihua-common.yaml rpc 段；无默认值=缺失启动失败）
    @Value("${rpc.signKey}")
    private String internalSignKey;

    /**
     * RestClient 统一配置，每个接口配置 RestClientFactoryBean
     */
    @Bean
    public RestClient.Builder restClientBuilder(LoadBalancerInterceptor loadBalancerInterceptor) {
        return RestClient
            .builder()
            .requestFactory(initRequestFactory())
            // 负载均衡拦截器
            .requestInterceptor(loadBalancerInterceptor)
            // 请求拦截器
            .requestInterceptor((request, body, execution) -> {
                // token 透传
                String token = LoginUserContext.getToken();
                if (StringUtils.hasText(token)) {
                    request.getHeaders().add(CustomHttpHeader.TOKEN.getValue(), token);
                }

                // traceId 透传（MDC 由入口 TraceIdFilter 写入）
                String traceId = MDC.get(TraceIdUtils.MDC_KEY);
                if (StringUtils.hasText(traceId)) {
                    request.getHeaders().add(CustomHttpHeader.TRACE_ID.getValue(), traceId);
                }

                // 原始请求的 ip 与客户端类型透传（仅请求线程传播；非请求线程无原始上下文可透传）
                if (WebUtils.getCurrentRequest() != null) {
                    String ipAddress = LoginUserContext.getIpAddress();
                    if (StringUtils.hasText(ipAddress)) {
                        request.getHeaders().add(CustomHttpHeader.IP.getValue(), ipAddress);
                    }
                    String clientType = LoginUserContext.getClientType();
                    if (StringUtils.hasText(clientType)) {
                        request.getHeaders().add(CustomHttpHeader.CLIENT_TYPE.getValue(), clientType);
                    }
                }

                // 生成签名
                long timeMillis = DateUtils.nowTimeStamp();
                String sign = HmacUtils.hmacSha256(internalSignKey, String.format("%s:%s:%s",
                        request.getMethod().name(),
                        request.getURI().getPath(),
                        timeMillis));
                request.getHeaders().add(CustomHttpHeader.SIGN.getValue(), sign);
                request.getHeaders().add(CustomHttpHeader.TIMESTAMP.getValue(), String.valueOf(timeMillis));

                return execution.execute(request, body);
            });
    }

    /**
     * 配置连接池与超时时间（客户端与连接池启动期一次性构建，配置变更需重启生效）
     */
    private HttpComponentsClientHttpRequestFactory initRequestFactory() {
        // 设置超时时间
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(clientProperties.getConnectTimeout()))
                .setResponseTimeout(Timeout.of(clientProperties.getResponseTimeout()))
                .setConnectionRequestTimeout(Timeout.of(clientProperties.getConnectionRequestTimeout()))
                .build();

        // 连接池参数显式化（不显式配置时 HC5 默认池仅 25 总连接/每路由 5，高峰期等池排队）
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(clientProperties.getMaxConnTotal())
                .setMaxConnPerRoute(clientProperties.getMaxConnPerRoute())
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(config)
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
