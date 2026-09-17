package com.lihua.client.config;

import com.lihua.common.enums.CustomHttpHeader;
import com.lihua.common.utils.crypt.HmacUtils;
import com.lihua.common.utils.date.DateUtils;
import com.lihua.security.manager.LoginUserContext;
import jakarta.annotation.Resource;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
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
     * 配置连接池与超时时间
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
