package com.lihua.gateway.exception;

import com.lihua.common.enums.ResultCodeEnum;
import com.lihua.common.exception.BaseException;

public class GatewayTokenIllegalException extends BaseException {
    public GatewayTokenIllegalException() {
        super(ResultCodeEnum.AUTHENTICATION_EXPIRED, "非法令牌", null);
    }
}
