package com.imooc.netty;

import lombok.Getter;

/**
 * @author hellozjf
 */
@Getter
public enum WebSocketResultEnum {

    UNSUPPORT_MESSAGE(1, "不支持消息"),
    ;

    Integer code;
    String message;
    WebSocketResultEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
