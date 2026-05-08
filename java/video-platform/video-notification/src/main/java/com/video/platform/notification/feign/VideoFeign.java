package com.video.platform.notification.feign;

import com.video.platform.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "video-video", url = "${feign.video-url:http://localhost:8084}")
public interface VideoFeign {
    
    @GetMapping("/video/simple")
    Result getVideoSimple(@RequestParam("id") Long id);
    
    @GetMapping("/api/comment/simple")
    Result getCommentSimple(@RequestParam("id") Long id);
}
