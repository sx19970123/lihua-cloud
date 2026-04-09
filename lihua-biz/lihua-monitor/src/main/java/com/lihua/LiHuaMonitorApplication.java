package com.lihua;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableDiscoveryClient
@EnableAsync(proxyTargetClass = true)
@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
@ComponentScan({"com.lihua.**"})
public class LiHuaMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(LiHuaMonitorApplication.class, args);
        System.out.println("""

                     __   _____ _______   _      _____  _______    _____ _    _  _____ _____ ______  _____ _____       __
                    / /  / ____|__   __| / \\    |  __ \\|__   __|  / ____| |  | |/ ____/ ____|  ____|/ ____/ ____|     / /
                   / /  | (___    | |   /   \\   | |__) |  | |    | (___ | |  | | |   | |    | |__  | (___| (___      / /\s
                  / /    \\___ \\   | |  / /_\\ \\  |  _  /   | |     \\___ \\| |  | | |   | |    |  __|  \\___ \\\\___ \\    / / \s
                 / /     ____) |  | | / _____ \\ | | \\ \\   | |     ____) | |__| | |___| |____| |____ ____) |___) |  / /\s
                /_/     |_____/   |_|/_/     \\_\\|_|  \\_\\  |_|    |_____/ \\____/ \\_____\\_____|______|_____/_____/  /_/\s
                """
        );
    }
}
