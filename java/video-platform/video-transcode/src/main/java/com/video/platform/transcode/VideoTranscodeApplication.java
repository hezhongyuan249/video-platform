package com.video.platform.transcode;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.video.platform.transcode.mapper")
@EnableFeignClients
public class VideoTranscodeApplication {
    public static void main(String[] args) {
        SpringApplication.run(VideoTranscodeApplication.class, args);
    }
}
