package com.lihua.security.config;

import com.lihua.security.filter.JwtAuthenticationTokenFilter;
import com.lihua.security.handler.LogoutSuccessHandlerImpl;
import com.lihua.security.handler.SecurityAccessDeniedHandler;
import com.lihua.security.handler.SecurityAuthenticationEntryPoint;
import jakarta.annotation.Resource;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Resource
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    @Resource
    private LogoutSuccessHandlerImpl logoutSuccessHandler;

    @Resource
    private SecurityAccessDeniedHandler securityAccessDeniedHandler;

    @Resource
    private SecurityAuthenticationEntryPoint securityAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {

        // 配置拦截请求
        http.authorizeHttpRequests(authorizeHttpRequestsCustomizer -> authorizeHttpRequestsCustomizer
                // 对于异步分发权限放开（涉及附件下载返回 ResponseEntity<StreamingResponseBody> 的情况）
                .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                // 后台接口配置
                .requestMatchers(
                        "/system/auth/login",                            // 登录
                        "/system/auth/register/**",                      // 注册
                        "/system/user/checkUserName/**",                 // 检查用户名
                        "/system/attachment/storage/download/**",        // 附件下载
                        "/system/setting/GrayModelSetting",              // 灰色模式设置
                        "/system/setting/base/**"                        // 基础设置
                ).permitAll()
                // 远程调用接口——@InternalOnly 与 permitAll 的搭配规则（签名为服务间信任凭证，与登录墙正交）：
                // 仅「调用时无用户 token」的场景入本清单由签名独扛（登录链在 token 签发前/注册/日志落库）；
                // 其余远程调用端点（如 system/setting/cacheIpBlack、system/dictData/queryByDictTypeCode——
                // 调用方恒带透传 token）保持 anyRequest().authenticated() 叠加签名墙，勿移入本清单
                .requestMatchers(
                        "/system/log/login/insert",                     // 登录日志记录
                        "/system/log/operate/insert",                   // 操作日志记录
                        "/system/user/auth/**"                          // 用户登录远程调用
                )
                .permitAll()
                // app接口配置
                .requestMatchers(
                        "/app/system/auth/login",                           // 登录
                        "/app/system/auth/register/**",                     // 注册
                        "/app/system/user/checkUserName/**",                // 检查用户名
                        "/app/system/attachment/storage/download/**",       // 附件下载
                        "/app/system/setting/base/**"                       // 基础设置
                ).permitAll()
                // 系统其他接口配置
                .requestMatchers(
                        "/captcha/**",                                  // 验证码
                        "/actuator/health/**",                          // 健康探针（compose healthcheck 经主端口探活；show-details 默认 never 仅暴露整体状态；网关无该前缀路由，仅内网/本机可达）
                        "/ws-connect/**",                               // websocket建立连接
                        "/swagger-ui/**",                               // spring-doc
                        "/v3/api-docs/**",                              // spring-doc
                        "/error"                                        // 当出现404等异常时spring内部会转发到/error，需要将其放过，否则会响应401
                ).permitAll()
                .anyRequest().authenticated());

        // 关闭csrf拦截
        http.csrf(AbstractHttpConfigurer::disable);

        // CORS 接入 Security 链：按名探测 base-web CorsConfig 的 corsConfigurationSource bean（唯一 CORS 源），
        // 使受保护接口的 OPTIONS 预检在认证前短路返回
        http.cors(Customizer.withDefaults());

        // 允许通过iframe访问
        http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));

        // 基于前后端分离token 认证 无需session
        http.sessionManagement(sessionManagementCustomizer -> sessionManagementCustomizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 添加 jwt token 验证过滤器
        http.addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);

        // 添加退出登录处理器
        http.logout(logoutCustomizer -> logoutCustomizer
                .logoutUrl("/logout")
                .logoutSuccessHandler(logoutSuccessHandler));

        // 添加权限/认证异常处理器
        http.exceptionHandling(exceptionHandlingCustomizer -> exceptionHandlingCustomizer
                .authenticationEntryPoint(securityAuthenticationEntryPoint)
                .accessDeniedHandler(securityAccessDeniedHandler));

        return http.build();
    }
}
