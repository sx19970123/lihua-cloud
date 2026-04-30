package com.lihua.api.registrar;

import com.lihua.api.annotation.RemoteClient;
import jakarta.annotation.Resource;
import lombok.Setter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

public class WebClientFactoryBean <T> implements FactoryBean<T> {

    @Setter
    private Class<T> type;

    private T singleton;

    @Resource
    private WebClient.Builder webClientBuilder;


    @Override
    public T getObject() {
        if (singleton == null) {
            singleton = createClient();
        }
        return singleton;
    }

    private T createClient() {
        RemoteClient annotation = type.getAnnotation(RemoteClient.class);

        if (!StringUtils.hasText(annotation.serverName())) {
            throw new IllegalStateException(type.getName() + " 服务名称为空");
        }

        WebClient client = webClientBuilder
                .clone()
                // 协议+服务名称
                .baseUrl(annotation.scheme().name().toLowerCase() + "://" + annotation.serverName())
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
