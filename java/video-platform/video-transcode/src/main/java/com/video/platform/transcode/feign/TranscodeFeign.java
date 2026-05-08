package com.video.platform.transcode.feign;

import com.video.platform.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "video-transcode", path = "/transcode")
public interface TranscodeFeign {

    @GetMapping("/progress/{videoId}")
    Result getProgress(@PathVariable("videoId") Long videoId);
    
    @PostMapping("/start/{videoId}")
    Result startTranscode(@PathVariable("videoId") Long videoId, 
                          @RequestParam("inputPath") String inputPath,
                          @RequestParam("baseName") String baseName);
    
    @PostMapping("/execute")
    Result executeTranscode(@RequestParam("videoId") Long videoId,
                            @RequestParam("inputPath") String inputPath,
                            @RequestParam("baseName") String baseName);
}
