package com.lihua.gateway.fallback;

import com.lihua.common.enums.ResultCodeEnum;
import com.lihua.common.model.response.ResponseModel;
import com.lihua.common.model.response.ResponseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
public class FallbackController extends ResponseController {

    @RequestMapping("/fallback")
    public Mono<ResponseModel<String>> fallback() {
        log.error("触发网关降级");
        return Mono.just(error(ResultCodeEnum.SYSTEM_ERROR));
    }

}
