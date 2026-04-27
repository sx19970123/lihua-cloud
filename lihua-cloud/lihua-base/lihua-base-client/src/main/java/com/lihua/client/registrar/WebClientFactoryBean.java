package com.lihua.client.registrar;

import com.lihua.client.annotation.HttpClient;
import com.lihua.client.config.HttpClientProperties;
import io.netty.channel.ChannelOption;
import jakarta.annotation.Resource;
import lombok.Setter;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;


import java.time.Duration;

public class WebClientFactoryBean <T> implements FactoryBean<T> {

    @Setter
    private Class<T> type;

    private T singleton;

    @Resource
    private WebClient.Builder webClientBuilder;

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

        // 设置超时时间
        reactor.netty.http.client.HttpClient httpClient = reactor.netty.http.client.HttpClient.create()
                // 连接超时
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                // 响应超时（read timeout）
                .responseTimeout(Duration.ofSeconds(5));

        WebClient client = webClientBuilder
                .clone()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // 协议+服务名称
                .baseUrl(annotation.scheme().name().toLowerCase() + "://" + annotation.value())
                .build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(client)).build();

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
