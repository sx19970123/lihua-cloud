package com.lihua.client.system;

import com.lihua.cloud.annotation.HttpClient;
import com.lihua.common.model.response.ApiResponseModel;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpClient("lihua-system")
@HttpExchange("/system/log")
public interface SysLogClient {


    @GetExchange("operate/{id}")
    ApiResponseModel<Object> querySysLog(@PathVariable String id);


}
