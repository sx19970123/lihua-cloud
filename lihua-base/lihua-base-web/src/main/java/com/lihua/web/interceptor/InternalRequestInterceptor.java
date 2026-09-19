package com.lihua.web.interceptor;

import com.lihua.common.enums.CustomHttpHeader;
import com.lihua.common.enums.ResultCodeEnum;
import com.lihua.common.model.response.response.StrResponse;
import com.lihua.common.utils.crypt.HmacUtils;
import com.lihua.web.utils.WebUtils;
import com.lihua.web.annotation.InternalOnly;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 搭配@InternalOnly注解，对目标 controller 进行拦截验证签名
 */
@Component
@Slf4j
public class InternalRequestInterceptor implements HandlerInterceptor {

    // 内部 RPC 签名密钥（来自 lihua-common.yaml rpc 段；无默认值=缺失启动失败）
    @Value("${rpc.signKey}")
    private String internalSignKey;

    // 签名时间窗毫秒（请求时间戳超出窗口即拒绝，防重放；容器间时钟漂移须小于该值）
    @Value("${rpc.signTimeout:10000}")
    private long signTimeout;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {

        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        InternalOnly internalOnly = method.getMethodAnnotation(InternalOnly.class);

        if (internalOnly == null) {
            return true;
        }

        // 验证内部调用请求是否超时（拒绝须留日志：HTTP 200 + code 401 的静默拒绝在上游被防枚举
        // 掩盖为凭据失败，是「正确密码偶发登录失败」且无迹可查的头号来源）
        String timestampStr = request.getHeader(CustomHttpHeader.TIMESTAMP.getValue());
        if (timestampStr == null) {
            log.warn("内部RPC签名校验拒绝（缺时间戳头）: {} {}", request.getMethod(), request.getRequestURI());
            WebUtils.renderJson(StrResponse.error(ResultCodeEnum.AUTHENTICATION_EXPIRED, "参数错误"));
            return false;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (Exception e) {
            log.warn("内部RPC签名校验拒绝（时间戳格式非法）: {} {}", request.getMethod(), request.getRequestURI());
            WebUtils.renderJson(StrResponse.error(ResultCodeEnum.AUTHENTICATION_EXPIRED, "参数错误"));
            return false;
        }

        if (Math.abs(System.currentTimeMillis() - timestamp) > signTimeout) {
            log.warn("内部RPC签名校验拒绝（时间戳超窗，疑容器时钟漂移超过{}ms）: {} {}",
                    signTimeout, request.getMethod(), request.getRequestURI());
            WebUtils.renderJson(StrResponse.error(ResultCodeEnum.AUTHENTICATION_EXPIRED, "签名过期"));
            return false;
        }

        String sign = request.getHeader(CustomHttpHeader.SIGN.getValue());

        // 生成确认签名
        String confirmSign = HmacUtils.hmacSha256(internalSignKey, String.format("%s:%s:%s",
                request.getMethod(),
                request.getRequestURI(),
                timestamp));

        // 签名对比
        if (!confirmSign.equals(sign)) {
            log.warn("内部RPC签名校验拒绝（签名不匹配，疑实例间signKey不一致）: {} {}",
                    request.getMethod(), request.getRequestURI());
            WebUtils.renderJson(StrResponse.error(ResultCodeEnum.AUTHENTICATION_EXPIRED, "签名错误"));
            return false;
        }

        return true;
    }
}
