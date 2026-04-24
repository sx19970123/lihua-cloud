package com.lihua.web.interceptor;

import com.lihua.common.enums.ResultCodeEnum;
import com.lihua.common.enums.SignEnum;
import com.lihua.common.model.response.response.StrResponse;
import com.lihua.common.utils.crypt.HmacUtils;
import com.lihua.web.utils.WebUtils;
import com.lihua.web.annotation.InternalOnly;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 搭配@InternalOnly注解，对目标 controller 进行拦截验证签名
 */
@Component
public class InternalRequestInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {

        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        InternalOnly internalOnly = method.getMethodAnnotation(InternalOnly.class);

        if (internalOnly == null) {
            return true;
        }

        // 验证内部调用请求是否超时
        String timestampStr = request.getHeader("Timestamp");
        if (timestampStr == null) {
            WebUtils.renderJson(StrResponse.error(ResultCodeEnum.AUTHENTICATION_EXPIRED, "参数错误"));
            return false;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (Exception e) {
            WebUtils.renderJson(StrResponse.error(ResultCodeEnum.AUTHENTICATION_EXPIRED, "参数错误"));
            return false;
        }

        if (Math.abs(System.currentTimeMillis() - timestamp) > internalOnly.timeout()) {
            WebUtils.renderJson(StrResponse.error(ResultCodeEnum.AUTHENTICATION_EXPIRED, "签名过期"));
            return false;
        }

        String sign = request.getHeader(SignEnum.SIGN_KEY.getValue());

        // 生成确认签名
        String confirmSign = HmacUtils.hmacSha256(SignEnum.SIGN_SECRET.getValue(), String.format("%s:%s:%s",
                request.getMethod(),
                request.getRequestURI(),
                timestamp));

        // 签名对比
        if (!confirmSign.equals(sign)) {
            WebUtils.renderJson(StrResponse.error(ResultCodeEnum.AUTHENTICATION_EXPIRED, "签名错误"));
            return false;
        }

        return true;
    }
}
