package com.lihua.client.registrar;

import com.lihua.client.annotation.RemoteClient;
import jakarta.annotation.Resource;
import lombok.Setter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

public class RestClientFactoryBean<T> implements FactoryBean<T> {

    @Setter
    private Class<T> type;

    private T singleton;

    @Resource
    private RestClient.Builder restClientBuilder;

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

        RestClient client = restClientBuilder
                .clone()
                // 协议+服务名称
                .baseUrl(annotation.scheme().name().toLowerCase() + "://" + annotation.serverName())
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