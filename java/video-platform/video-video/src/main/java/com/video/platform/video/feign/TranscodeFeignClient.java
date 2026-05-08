package com.video.platform.video.feign;

import com.video.platform.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "video-transcode", path = "/transcode")
public interface TranscodeFeignClient {

    @GetMapping("/progress/{videoId}")
    Result getProgress(@PathVariable("videoId") Long videoId);
    
    @PostMapping("/execute")
    Result executeTranscode(@RequestParam("videoId") Long videoId,
                           @RequestParam("inputPath") String inputPath,
                           @RequestParam("baseName") String baseName);
}
