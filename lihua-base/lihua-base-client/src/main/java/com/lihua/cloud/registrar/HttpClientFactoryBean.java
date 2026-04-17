package com.lihua.cloud.registrar;

import com.lihua.cloud.annotation.HttpClient;
import com.lihua.cloud.config.HttpClientProperties;
import jakarta.annotation.Resource;
import lombok.Setter;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import java.time.Duration;

public class HttpClientFactoryBean<T> implements FactoryBean<T> {

    @Setter
    private Class<T> type;

    private T singleton;

    @Resource
    private RestClient.Builder restClientBuilder;

    @Resource
    private HttpClientProperties httpClientProperties;

    @Resource
    private CloseableHttpClient httpClient;

    @Override
    public T getObject() {
        if (singleton == null) {
            singleton = createClient();
        }
        return singleton;
    }

    private T createClient() {
        HttpClient annotation = type.getAnnotation(HttpClient.class);

        // 访问超时时间
        int readTimeout = annotation.readTimeout();
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        httpRequestFactory.setReadTimeout(readTimeout == -1 ? httpClientProperties.getReadTimeout() : Duration.ofSeconds(readTimeout));

        if (!StringUtils.hasText(annotation.value())) {
            throw new IllegalStateException(type.getName() + " 服务名称为空");
        }

        RestClient client = restClientBuilder
                .clone()
                // 超时时间
                .requestFactory(httpRequestFactory)
                // 协议+服务名称
                .baseUrl(annotation.scheme().name().toLowerCase() + "://" + annotation.value())
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build();

        return factory.createClient(type);
    }

    @Override
    public Class<?> getObjectType() {
        return this.type;
    }

    @Override
    public boolean isSingleton() {
        return FactoryBean.super.isSingleton();
    }
}