package com.lihua.client.config;

import com.lihua.common.enums.SignEnum;
import com.lihua.common.enums.TokenEnum;
import com.lihua.common.utils.crypt.HmacUtils;
import com.lihua.common.utils.date.DateUtils;
import com.lihua.security.manager.LoginUserContext;
import io.netty.channel.ChannelOption;
import jakarta.annotation.Resource;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Resource
    private ClientProperties clientProperties;

    /**
     * WebClient 统一配置，每个接口配置 WebClientFactoryBean
     */
    @Bean
    public WebClient.Builder webClientBuilder(ReactorLoadBalancerExchangeFilterFunction filterFunction) {
        return WebClient
            .builder()
            .clientConnector(initConnector())
            // 负载均衡过滤器
            .filter(filterFunction)
            // 透传
            .filter((request, next) -> {
                // token透传
                String token = LoginUserContext.getToken();

                //  构建新的 request
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

                return next.exchange(builder.build());
            });
    }

    /**
     * 配置连接/超时时间
     */
    private ReactorClientHttpConnector initConnector() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(clientProperties.getConnectTimeout().toMillis()))
                .responseTimeout(clientProperties.getResponseTimeout());
        return new ReactorClientHttpConnector(httpClient);
    }
}
