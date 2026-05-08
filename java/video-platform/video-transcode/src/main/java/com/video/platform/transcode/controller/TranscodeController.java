package com.video.platform.transcode.controller;

import com.video.platform.common.result.Result;
import com.video.platform.transcode.service.TranscodeService;
import com.video.platform.transcode.util.TranscodeProgress;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/transcode")
public class TranscodeController {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    @Resource
    private TranscodeService transcodeService;

    @GetMapping("/progress/{videoId}")
    public Result getProgress(@PathVariable Long videoId) {
        TranscodeProgress progress = TranscodeProgress.get(videoId);
        if (progress == null) {
            return Result.success(null);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("videoId", progress.getVideoId());
        result.put("progress", progress.getOverallProgress());
        result.put("status", progress.getStatus());
        result.put("progress480p", progress.getProgress480p());
        result.put("progress720p", progress.getProgress720p());
        result.put("progress1080p", progress.getProgress1080p());
        result.put("task480p", progress.getTask480p());
        result.put("task720p", progress.getTask720p());
        result.put("task1080p", progress.getTask1080p());
        return Result.success(result);
    }
    
    @PostMapping("/execute")
    public Result executeTranscode(@RequestParam Long videoId,
                                   @RequestParam String inputPath,
                                   @RequestParam String baseName) {
        if (TranscodeProgress.hasProgress(videoId)) {
            return Result.error("该视频已在转码中");
        }
        
        executor.submit(() -> {
            try {
                transcodeService.transcodeVideo(videoId, inputPath, baseName);
            } catch (Exception e) {
                System.err.println("转码执行失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        return Result.success("转码任务已启动");
    }
}
