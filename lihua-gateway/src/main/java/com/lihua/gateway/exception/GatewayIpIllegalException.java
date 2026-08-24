package com.lihua.gateway.exception;

import com.lihua.common.enums.ResultCodeEnum;
import com.lihua.common.exception.BaseException;

/**
 * ip 非法异常
 */
public class GatewayIpIllegalException extends BaseException {
    public GatewayIpIllegalException() {
        super(ResultCodeEnum.IP_ILLEGAL_ERROR, null);
    }
}