package com.lihua.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SignEnum {

    // 签名header key
    SIGN_KEY("Internal-Sign");

    private final String value;
}
