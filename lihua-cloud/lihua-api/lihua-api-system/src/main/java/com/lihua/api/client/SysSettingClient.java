package com.lihua.api.client;

import com.lihua.api.annotation.RemoteClient;
import com.lihua.common.model.response.ApiResponseModel;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@RemoteClient(serverName = "lihua-system")
@HttpExchange("system/setting")
public interface SysSettingClient {

    /**
     * 缓存ip黑名单
     */
    @PostExchange("cacheIpBlack")
    ApiResponseModel<String> cacheIpBlack();

    /**
     * 查询同账号最大登录数
     */
    @GetExchange("base/getMaxConcurrentLogins")
    ApiResponseModel<Integer> getMaxConcurrentLogins();

    /**
     * 获取是否启用验证码
     */
    @GetExchange("base/enableCaptcha")
    ApiResponseModel<Boolean> enableCaptcha();

}
