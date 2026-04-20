package com.lihua.api.client;

import com.lihua.api.annotation.HttpClient;
import com.lihua.common.model.response.ApiResponseModel;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpClient("lihua-system")
@HttpExchange("/system/setting")
public interface SysSettingClient {

    /**
     * 缓存ip黑名单
     */
    @PostExchange("cacheIpBlack")
    ApiResponseModel<String> cacheIpBlack();

}
