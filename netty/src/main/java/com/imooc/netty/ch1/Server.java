package com.imooc.netty.ch1;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author hellozjf
 */
@Data
@Slf4j
public class Server {

    private ServerSocket serverSocket;

    public Server(int port) {
        try {
            this.serverSocket = new ServerSocket(port);
            log.info("服务端启动成功，端口：{}", port);
        } catch (IOException e) {
            log.error("服务端启动失败：{}", e);
        }
    }

    public void start() {
        new Thread(() -> {
            doStart();
        }).start();
    }

    private void doStart() {
        while (true) {
            try {
                Socket client = serverSocket.accept();
                new ClientHandler(client).start();
            } catch (IOException e) {
                log.error("服务端异常：{}", e);
            }
        }
    }
}
