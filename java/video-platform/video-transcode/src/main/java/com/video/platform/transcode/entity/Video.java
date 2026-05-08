package com.video.platform.transcode.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

@TableName("tb_video")
public class Video {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String intro;
    @TableField("video_url")
    private String videoUrl;
    @TableField("video_url_480p")
    private String videoUrl480p;
    @TableField("video_url_720p")
    private String videoUrl720p;
    @TableField("video_url_1080p")
    private String videoUrl1080p;
    @TableField("cover_url")
    private String coverUrl;
    @TableField("duration")
    private Integer duration;
    @TableField("user_id")
    private Long userId;
    private String username;
    @TableField("create_time")
    private Date createTime;
    private Integer status;
    @TableField("video_type")
    private String videoType;
    private Integer likes = 0;
    private Integer favorites = 0;
    private Integer views = 0;
    
    @TableField("review_status")
    private Integer reviewStatus;
    @TableField("review_time")
    private Date reviewTime;
    @TableField("reviewer_id")
    private Long reviewerId;
    @TableField("reviewer_name")
    private String reviewerName;
    @TableField("reject_reason")
    private String rejectReason;
    
    @TableField("pending_title")
    private String pendingTitle;
    @TableField("pending_intro")
    private String pendingIntro;
    @TableField("pending_cover_url")
    private String pendingCoverUrl;
    @TableField("pending_video_type")
    private String pendingVideoType;
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getIntro() {
        return intro;
    }
    
    public void setIntro(String intro) {
        this.intro = intro;
    }
    
    public String getVideoUrl() {
        return videoUrl;
    }
    
    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
    
    public String getVideoUrl480p() {
        return videoUrl480p;
    }
    
    public void setVideoUrl480p(String videoUrl480p) {
        this.videoUrl480p = videoUrl480p;
    }
    
    public String getVideoUrl720p() {
        return videoUrl720p;
    }
    
    public void setVideoUrl720p(String videoUrl720p) {
        this.videoUrl720p = videoUrl720p;
    }
    
    public String getVideoUrl1080p() {
        return videoUrl1080p;
    }
    
    public void setVideoUrl1080p(String videoUrl1080p) {
        this.videoUrl1080p = videoUrl1080p;
    }
    
    public String getCoverUrl() {
        return coverUrl;
    }
    
    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public Date getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public String getVideoType() {
        return videoType;
    }
    
    public void setVideoType(String videoType) {
        this.videoType = videoType;
    }
    
    public Integer getLikes() {
        return likes;
    }
    
    public void setLikes(Integer likes) {
        this.likes = likes;
    }
    
    public Integer getFavorites() {
        return favorites;
    }
    
    public void setFavorites(Integer favorites) {
        this.favorites = favorites;
    }
    
    public Integer getViews() {
        return views;
    }
    
    public void setViews(Integer views) {
        this.views = views;
    }
    
    public Integer getReviewStatus() {
        return reviewStatus;
    }
    
    public void setReviewStatus(Integer reviewStatus) {
        this.reviewStatus = reviewStatus;
    }
    
    public Date getReviewTime() {
        return reviewTime;
    }
    
    public void setReviewTime(Date reviewTime) {
        this.reviewTime = reviewTime;
    }
    
    public Long getReviewerId() {
        return reviewerId;
    }
    
    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }
    
    public String getReviewerName() {
        return reviewerName;
    }
    
    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }
    
    public String getRejectReason() {
        return rejectReason;
    }
    
    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
    
    public String getPendingTitle() {
        return pendingTitle;
    }
    
    public void setPendingTitle(String pendingTitle) {
        this.pendingTitle = pendingTitle;
    }
    
    public String getPendingIntro() {
        return pendingIntro;
    }
    
    public void setPendingIntro(String pendingIntro) {
        this.pendingIntro = pendingIntro;
    }
    
    public String getPendingCoverUrl() {
        return pendingCoverUrl;
    }
    
    public void setPendingCoverUrl(String pendingCoverUrl) {
        this.pendingCoverUrl = pendingCoverUrl;
    }
    
    public String getPendingVideoType() {
        return pendingVideoType;
    }
    
    public void setPendingVideoType(String pendingVideoType) {
        this.pendingVideoType = pendingVideoType;
    }
}
