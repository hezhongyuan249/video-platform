package com.video.platform.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

// 核心：排除Redis，解决500错误
@SpringBootApplication(exclude = {RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class})
@EnableDiscoveryClient
@MapperScan("com.video.platform.user.mapper")
@EnableScheduling
public class VideoUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(VideoUserApplication.class, args);
    }
}