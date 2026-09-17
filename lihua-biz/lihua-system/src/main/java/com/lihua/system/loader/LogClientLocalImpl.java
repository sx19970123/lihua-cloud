package com.lihua.system.loader;

import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.log.client.LogClient;
import com.lihua.log.model.LogModel;
import com.lihua.system.service.SysLogService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 日志落库的本地通道：sys_log 表 Owner 直写，@ComponentScan 自动注册。
 * @Primary：system 进程同时引入 api-system 的 RPC 版实现（SysLogClientFacade），本地直写优先
 */
@Component
@Primary
public class LogClientLocalImpl implements LogClient {

    // 操作日志service
    @Resource(name = "sysOperateLogService")
    private SysLogService sysOperateLogService;

    // 登录日志service
    @Resource(name = "sysLoginLogService")
    private SysLogService sysLoginLogService;

    @Override
    public Mono<ApiResponseModel<String>> insertOperate(LogModel logModel) {
        sysOperateLogService.insert(logModel);
        return Mono.empty();
    }

    @Override
    public Mono<ApiResponseModel<String>> insertLogin(LogModel logModel) {
        sysLoginLogService.insert(logModel);
        return Mono.empty();
    }
}
