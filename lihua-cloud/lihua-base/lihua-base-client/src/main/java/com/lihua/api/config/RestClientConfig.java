package com.lihua.api.config;

import com.lihua.common.enums.SignEnum;
import com.lihua.common.utils.crypt.HmacUtils;
import com.lihua.common.utils.date.DateUtils;
import com.lihua.common.enums.TokenEnum;
import com.lihua.security.manager.LoginUserContext;
import jakarta.annotation.Resource;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
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
                    request.getHeaders().add(TokenEnum.TOKEN_KEY.getValue(), token);
                }

                // 生成签名
                long timeMillis = DateUtils.nowTimeStamp();
                String sign = HmacUtils.hmacSha256(SignEnum.SIGN_SECRET.getValue(), String.format("%s:%s:%s",
                        request.getMethod().name(),
                        request.getURI().getPath(),
                        timeMillis));
                request.getHeaders().add(SignEnum.SIGN_KEY.getValue(), sign);
                request.getHeaders().add("Timestamp", String.valueOf(timeMillis));

                return execution.execute(request, body);
            });
    }

    /**
     * 配置连接/超时时间
     */
    private HttpComponentsClientHttpRequestFactory initRequestFactory() {
        // 设置超时时间
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(clientProperties.getConnectTimeout()))
                .setResponseTimeout(Timeout.of(clientProperties.getResponseTimeout()))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
