package com.lihua.client.client;

import com.lihua.client.annotation.RemoteClient;
import com.lihua.common.model.response.ResponseModel;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

@RemoteClient(serverName = "lihua-system")
@HttpExchange("system/setting")
public interface SysSettingClient {

    /**
     * 缓存ip黑名单
     */
    @PostExchange("cacheIpBlack")
    ResponseModel<String> cacheIpBlack();

    /**
     * 查询同账号最大登录数
     */
    @GetExchange("base/getMaxConcurrentLogins")
    ResponseModel<Integer> getMaxConcurrentLogins();

    /**
     * 获取是否启用验证码
     */
    @GetExchange("base/enableCaptcha")
    ResponseModel<Boolean> enableCaptcha();

    /**
     * 获取当前请求端验证码随机类型池
     */
    @GetExchange("base/getCaptchaTypes")
    ResponseModel<List<String>> getCaptchaTypes();

}
