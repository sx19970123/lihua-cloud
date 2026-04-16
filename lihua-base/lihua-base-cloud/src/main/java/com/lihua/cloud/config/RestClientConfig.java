package com.lihua.cloud.config;

import com.lihua.security.enums.TokenEnum;
import com.lihua.security.manager.LoginUserContext;
import jakarta.annotation.Resource;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class RestClientConfig {

    @Resource
    private HttpClientProperties httpClientProperties;

    @Bean
    @LoadBalanced
    public RestClient.Builder restClientBuilder() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        // 连接超时
        factory.setConnectionRequestTimeout(httpClientProperties.getConnectTimeout());
        // 读取超时
        factory.setReadTimeout(httpClientProperties.getReadTimeout());

        return RestClient
                .builder()
                .requestFactory(factory)
                // 请求拦截器
                .requestInterceptor((request, body, execution) -> {
                    // token 透传
                    String token = LoginUserContext.getToken();
                    if (StringUtils.hasText(token)) {
                        request.getHeaders().add(TokenEnum.TOKEN_KEY.getValue(), token);
                    }
                    return execution.execute(request, body);
                });
    }

    /**
     * 创建httpInterface代理对象
     */
    public <T> T httpInterface(Class<T> clazz, RestClient.Builder restClientBuilder) {
        // 设置baseURL
        RestClient restClient = restClientBuilder.baseUrl("").build();

        // 创建 HttpServiceProxyFactory
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        // 生成代理对象
        return factory.createClient(clazz);
    }
}

