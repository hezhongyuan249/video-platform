package com.video.platform.transcode.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.video.platform.transcode.mapper.VideoMapper;
import com.video.platform.transcode.util.FFmpegUtil;
import com.video.platform.transcode.util.TranscodeProgress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;

@Service
public class TranscodeService {

    @Resource
    private FFmpegUtil ffmpegUtil;
    
    @Resource
    private VideoMapper videoMapper;
    
    @Value("${video.path:D:/video-platform/videos/}")
    private String VIDEO_PATH;
    
    @Value("${video.url.prefix:http://localhost:8080/videos/}")
    private String VIDEO_URL_PREFIX;

    public void transcodeVideo(Long videoId, String inputPath, String baseName) {
        TranscodeProgress progress = new TranscodeProgress(videoId);
        progress.setStatus("transcoding");
        TranscodeProgress.put(videoId, progress);

        String url480p = "";
        String url720p = "";
        String url1080p = "";

        try {
            progress.setTask480p("480P转码中");
            progress.setProgress480p(0);

            String path480p = VIDEO_PATH + baseName + "_480p.mp4";
            if (ffmpegUtil.transcodeVideo(inputPath, path480p, "480p", progress, "480p")) {
                url480p = VIDEO_URL_PREFIX + baseName + "_480p.mp4";
                progress.setTask480p("480P完成");
            } else {
                progress.setTask480p("480P失败");
            }
            progress.setProgress480p(100);

            progress.setTask720p("720P转码中");
            progress.setProgress720p(0);

            String path720p = VIDEO_PATH + baseName + "_720p.mp4";
            if (ffmpegUtil.transcodeVideo(inputPath, path720p, "720p", progress, "720p")) {
                url720p = VIDEO_URL_PREFIX + baseName + "_720p.mp4";
                progress.setTask720p("720P完成");
            } else {
                progress.setTask720p("720P失败");
            }
            progress.setProgress720p(100);

            progress.setTask1080p("1080P转码中");
            progress.setProgress1080p(0);

            String path1080p = VIDEO_PATH + baseName + "_1080p.mp4";
            if (ffmpegUtil.transcodeVideo(inputPath, path1080p, "1080p", progress, "1080p")) {
                url1080p = VIDEO_URL_PREFIX + baseName + "_1080p.mp4";
                progress.setTask1080p("1080P完成");
            } else {
                progress.setTask1080p("1080P失败");
            }
            progress.setProgress1080p(100);

            progress.setStatus("completed");

            com.video.platform.transcode.entity.Video updateVideo = new com.video.platform.transcode.entity.Video();
            updateVideo.setId(videoId);
            updateVideo.setVideoUrl480p(url480p);
            updateVideo.setVideoUrl720p(url720p);
            updateVideo.setVideoUrl1080p(url1080p);
            if (!url1080p.isEmpty()) {
                updateVideo.setVideoUrl(url1080p);
            }
            videoMapper.updateById(updateVideo);

            new File(inputPath).delete();

            TranscodeProgress.remove(videoId);

        } catch (Exception e) {
            progress.setStatus("failed");
        }
    }
}
