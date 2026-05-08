package com.video.platform.transcode.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class TranscodeProgress {
    
    private static final Map<Long, TranscodeProgress> progressMap = new ConcurrentHashMap<>();
    
    private Long videoId;
    private String status;
    private long startTime;
    private long endTime;
    
    private int progress480p;
    private int progress720p;
    private int progress1080p;
    private String task480p;
    private String task720p;
    private String task1080p;
    
    public TranscodeProgress(Long videoId) {
        this.videoId = videoId;
        this.progress480p = 0;
        this.progress720p = 0;
        this.progress1080p = 0;
        this.status = "pending";
        this.task480p = "等待中";
        this.task720p = "等待中";
        this.task1080p = "等待中";
        this.startTime = System.currentTimeMillis();
    }
    
    public static void put(Long videoId, TranscodeProgress progress) {
        progressMap.put(videoId, progress);
    }
    
    public static TranscodeProgress get(Long videoId) {
        return progressMap.get(videoId);
    }
    
    public static void remove(Long videoId) {
        progressMap.remove(videoId);
    }
    
    public static boolean hasProgress(Long videoId) {
        return progressMap.containsKey(videoId);
    }
    
    public int getOverallProgress() {
        return (progress480p + progress720p + progress1080p) / 3;
    }
    
    public Long getVideoId() {
        return videoId;
    }
    
    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    public int getProgress480p() {
        return progress480p;
    }
    
    public void setProgress480p(int progress480p) {
        this.progress480p = progress480p;
    }
    
    public int getProgress720p() {
        return progress720p;
    }
    
    public void setProgress720p(int progress720p) {
        this.progress720p = progress720p;
    }
    
    public int getProgress1080p() {
        return progress1080p;
    }
    
    public void setProgress1080p(int progress1080p) {
        this.progress1080p = progress1080p;
    }
    
    public String getTask480p() {
        return task480p;
    }
    
    public void setTask480p(String task480p) {
        this.task480p = task480p;
    }
    
    public String getTask720p() {
        return task720p;
    }
    
    public void setTask720p(String task720p) {
        this.task720p = task720p;
    }
    
    public String getTask1080p() {
        return task1080p;
    }
    
    public void setTask1080p(String task1080p) {
        this.task1080p = task1080p;
    }
}
