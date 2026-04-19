package com.lihua.api.registrar;

import com.lihua.api.annotation.EnableHttpClients;
import com.lihua.api.annotation.HttpClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import java.util.Set;

@Slf4j
public class HttpClientRegistrar implements ImportBeanDefinitionRegistrar {

    @SneakyThrows
    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata, @NonNull BeanDefinitionRegistry registry) {

        // 获取自定义注解 EnableHttpClients 中定义的包名
        AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(EnableHttpClients.class.getName()));
        if (annotationAttributes == null) {
            return;
        }

        String[] packages = annotationAttributes.getStringArray("packages");

        if (packages.length == 0) {
            return;
        }

        // 创建注解扫描器，扫描 @EnableHttpClients 下指定路径标记 @HttpClient 注解的类
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isIndependent();
            }
        };

        scanner.addIncludeFilter(new AnnotationTypeFilter(HttpClient.class));
        for (String pkg: packages) {
            Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(pkg);
            for (BeanDefinition beanDefinition : candidateComponents) {
                // 扫描到的包路径
                String className = beanDefinition.getBeanClassName();
                Class<?> clazz = Class.forName(className);
                // 使用FactoryBean进行创建注册
                BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(HttpClientFactoryBean.class);
                builder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
                builder.addPropertyValue("type", clazz);
                registry.registerBeanDefinition(clazz.getName(), builder.getBeanDefinition());
                log.info("扫描到 HttpExchange 接口，路径为：{}", className);
            }
        }

    }
}
