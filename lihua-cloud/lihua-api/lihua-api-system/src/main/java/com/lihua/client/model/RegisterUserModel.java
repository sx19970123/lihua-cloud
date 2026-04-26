package com.lihua.client.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户注册对象
 */
@Data
@Accessors(chain = true)
public class RegisterUserModel {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 状态
     */
    private String status;

    /**
     * 注册类型
     */
    private String registerType;

    /**
     * 密码更新时间
     */
    private LocalDateTime passwordUpdateTime;


}
