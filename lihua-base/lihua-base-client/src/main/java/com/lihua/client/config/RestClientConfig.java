package com.lihua.client.config;

import com.lihua.client.annotation.RemoteClient;
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

import java.time.Duration;

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
            // 基底默认超时（FactoryBean clone 后按接口注解覆盖；防未覆盖路径拿到无超时配置）
            .requestFactory(requestFactory(Duration.ofSeconds(RemoteClient.TIMEOUT_DEFAULT)))
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
     * 共享 HTTP 客户端（持连接池，全接口唯一一份）——各接口差异化超时经 requestFactory(responseTimeout)
     * 在 factory 层实现，连接池不随接口数增长
     */
    @Bean(destroyMethod = "close")
    public CloseableHttpClient sharedHttpClient() {
        // 默认配置仅连接族超时（网络环境特征全接口统一）；响应等待超时是接口特征，由各接口 factory 级配置覆盖
        RequestConfig defaultConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(clientProperties.getConnectTimeout()))
                .setConnectionRequestTimeout(Timeout.of(clientProperties.getConnectionRequestTimeout()))
                .build();

        // 连接池参数显式化（不显式配置时 HC5 默认池仅 25 总连接/每路由 5，高峰期等池排队）
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(clientProperties.getMaxConnTotal())
                .setMaxConnPerRoute(clientProperties.getMaxConnPerRoute())
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(defaultConfig)
                .build();
    }

    /**
     * 构建指定响应等待超时的请求工厂（连接族超时与池均取共享客户端的默认配置）。
     * 供 RestClientFactoryBean 按 @RemoteClient(timeout) 逐接口调用；客户端与连接池启动期一次性构建，配置变更需重启生效
     */
    public HttpComponentsClientHttpRequestFactory requestFactory(Duration responseTimeout) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(sharedHttpClient());
        // readTimeout 即 HC5 的 responseTimeout，随每次请求合并进共享客户端默认配置生效
        factory.setReadTimeout(responseTimeout);
        return factory;
    }
}
