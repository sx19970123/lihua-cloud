package com.lihua.cloud.config;

import com.lihua.common.utils.web.WebUtils;
import com.lihua.security.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

//    @Bean
//    @LoadBalanced
//    public RestClient restClientBuilder() {
//        return RestClient
//                .builder()
//                .requestInterceptor((request, body, execution) -> {})
//                .build();
//    }

//    /**
//     * 创建 UserHttpInterface 代理对象
//     */
//    public UserHttpInterface userHttpInterface(RestClient.Builder loadBalancedRestClientBuilder) {
//
//        // 构建 RestClient
//        RestClient restClient = loadBalancedRestClientBuilder
//                .baseUrl("http://your-service-name")  // 关键：使用服务名，如 http://user-service
//                .build();
//
//        // 创建 HttpServiceProxyFactory
//        HttpServiceProxyFactory factory = HttpServiceProxyFactory
//                .builderFor(RestClientAdapter.create(restClient))
//                .build();
//
//        // 生成代理对象
//        return factory.createClient(UserHttpInterface.class);
//    }
}

