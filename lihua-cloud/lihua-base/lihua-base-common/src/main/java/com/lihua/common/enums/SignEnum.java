package com.lihua.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SignEnum {

    // 签名密钥
    SIGN_SECRET("9f1c2e3d4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1"),

    // 签名header key
    SIGN_KEY("Internal-Sign");

    private final String value;
}
