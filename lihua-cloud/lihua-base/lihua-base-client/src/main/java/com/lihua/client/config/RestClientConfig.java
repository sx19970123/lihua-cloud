package com.lihua.client.config;

import com.lihua.client.annotation.EnableHttpClients;
import com.lihua.common.enums.SignEnum;
import com.lihua.common.utils.crypt.HmacUtils;
import com.lihua.common.utils.date.DateUtils;
import com.lihua.common.enums.TokenEnum;
import com.lihua.security.manager.LoginUserContext;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableHttpClients(packages = "com.lihua")
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder(LoadBalancerInterceptor loadBalancerInterceptor) {
        return RestClient
            .builder()
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

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder(ReactorLoadBalancerExchangeFilterFunction filterFunction) {
        return WebClient.builder().filter(filterFunction).filter((request,  next) -> {
            // token透传
            String token = LoginUserContext.getToken();
            ClientRequest.Builder builder = ClientRequest.from(request);

            if (StringUtils.hasText(token)) {
                builder.header(TokenEnum.TOKEN_KEY.getValue(), token);
            }

            // 签名
            long timeMillis = DateUtils.nowTimeStamp();
            String sign = HmacUtils.hmacSha256(
                    SignEnum.SIGN_SECRET.getValue(),
                    String.format("%s:%s:%s",
                            request.method().name(),
                            request.url().getPath(),
                            timeMillis)
            );

            builder.header(SignEnum.SIGN_KEY.getValue(), sign);
            builder.header("Timestamp", String.valueOf(timeMillis));

            //  构建新的 request
            ClientRequest newRequest = builder.build();

            return next.exchange(newRequest);
        });
    }

    @Bean
    public CloseableHttpClient httpClient() {
        return HttpClients.createDefault();
    }

}
