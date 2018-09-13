package com.imooc.netty;

/**
 * @author hellozjf
 */
public class WebSocketException extends RuntimeException {

    private Integer code;

    public WebSocketException(WebSocketResultEnum resultEnum) {
        super(resultEnum.message);
        this.code = resultEnum.getCode();
    }
}
