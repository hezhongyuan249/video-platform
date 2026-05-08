package com.video.platform.video;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 视频业务模块启动类
 */
@SpringBootApplication
@EnableDiscoveryClient  // Nacos服务注册
@EnableFeignClients  // 启用Feign远程调用
@MapperScan({"com.video.platform.video.mapper", "com.video.platform.user.mapper", "com.video.platform.common.mapper"}) // 扫描Mapper
public class VideoApplication {
    public static void main(String[] args) {
        SpringApplication.run(VideoApplication.class, args);
    }
}