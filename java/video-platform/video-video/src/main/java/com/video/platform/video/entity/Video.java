package com.video.platform.video.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

@TableName("tb_video")
public class Video {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;        // 标题
    private String intro;        // 简介
    @TableField("video_url")
    private String videoUrl;    // 视频地址
    @TableField("video_url_480p")
    private String videoUrl480p;    // 480P视频地址
    @TableField("video_url_720p")
    private String videoUrl720p;    // 720P视频地址
    @TableField("video_url_1080p")
    private String videoUrl1080p;   // 1080P视频地址
    @TableField("cover_url")
    private String coverUrl;    // 封面地址
    @TableField("duration")
    private Integer duration;  // 视频时长（秒）
    @TableField("user_id")
    private Long userId;        // 用户ID
    private String username;    // 用户名
    @TableField("create_time")
    private Date createTime;    // 创建时间
    private Integer status;     // 0正常 1下架
    @TableField("video_type")
    private String videoType;   // 视频类型
    private Integer likes = 0;      // 点赞数
    private Integer favorites = 0;  // 收藏数
    private Integer views = 0;      // 播放数
    
    // 审核相关字段
    @TableField("review_status")
    private Integer reviewStatus;  // 0待审核 1审核通过 2审核拒绝
    @TableField("review_time")
    private Date reviewTime;      // 审核时间
    @TableField("reviewer_id")
    private Long reviewerId;      // 审核人ID
    @TableField("reviewer_name")
    private String reviewerName;  // 审核人用户名
    @TableField("reject_reason")
    private String rejectReason;  // 拒绝原因
    
    // 待审核的修改数据（编辑后未审核通过的新数据）
    @TableField("pending_title")
    private String pendingTitle;  // 待审核的新标题
    @TableField("pending_intro")
    private String pendingIntro;  // 待审核的新简介
    @TableField("pending_cover_url")
    private String pendingCoverUrl;  // 待审核的新封面
    @TableField("pending_video_type")
    private String pendingVideoType; // 待审核的新分类
    @TableField("pending_video_url")
    private String pendingVideoUrl; // 待审核的新视频地址
    @TableField("has_pending_edit")
    private Boolean hasPendingEdit; // 是否有待审核的修改
    
    // 作者信息（不映射数据库，仅用于返回）
    @TableField(exist = false)
    private String userAvatar;
    @TableField(exist = false)
    private String userDescription;
    @TableField(exist = false)
    private Boolean following;
    @TableField(exist = false)
    private Integer videosCount;
    @TableField(exist = false)
    private Integer likesCount;
    @TableField(exist = false)
    private Integer favoritesCount;
    @TableField(exist = false)
    private Integer followersCount;
    @TableField(exist = false)
    private String categoryName;  // 分类名称（不映射数据库）

    // 无参构造
    public Video() {
    }

    // ================ Getter & Setter ================
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
    
    public Integer getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(Integer reviewStatus) { this.reviewStatus = reviewStatus; }
    public Date getReviewTime() { return reviewTime; }
    public void setReviewTime(Date reviewTime) { this.reviewTime = reviewTime; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    
    public String getPendingTitle() { return pendingTitle; }
    public void setPendingTitle(String pendingTitle) { this.pendingTitle = pendingTitle; }
    public String getPendingIntro() { return pendingIntro; }
    public void setPendingIntro(String pendingIntro) { this.pendingIntro = pendingIntro; }
    public String getPendingCoverUrl() { return pendingCoverUrl; }
    public void setPendingCoverUrl(String pendingCoverUrl) { this.pendingCoverUrl = pendingCoverUrl; }
    public String getPendingVideoType() { return pendingVideoType; }
    public void setPendingVideoType(String pendingVideoType) { this.pendingVideoType = pendingVideoType; }
    public String getPendingVideoUrl() { return pendingVideoUrl; }
    public void setPendingVideoUrl(String pendingVideoUrl) { this.pendingVideoUrl = pendingVideoUrl; }
    public Boolean getHasPendingEdit() { return hasPendingEdit; }
    public void setHasPendingEdit(Boolean hasPendingEdit) { this.hasPendingEdit = hasPendingEdit; }
    
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public String getUserDescription() { return userDescription; }
    public void setUserDescription(String userDescription) { this.userDescription = userDescription; }
    public Boolean getFollowing() { return following; }
    public void setFollowing(Boolean following) { this.following = following; }
    public Integer getVideosCount() { return videosCount; }
    public void setVideosCount(Integer videosCount) { this.videosCount = videosCount; }
    public Integer getLikesCount() { return likesCount; }
    public void setLikesCount(Integer likesCount) { this.likesCount = likesCount; }
    public Integer getFavoritesCount() { return favoritesCount; }
    public void setFavoritesCount(Integer favoritesCount) { this.favoritesCount = favoritesCount; }
    public Integer getFollowersCount() { return followersCount; }
    public void setFollowersCount(Integer followersCount) { this.followersCount = followersCount; }
    
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    // ================ toString ================
    @Override
    public String toString() {
        return "Video{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", intro='" + intro + '\'' +
                ", videoUrl='" + videoUrl + '\'' +
                ", coverUrl='" + coverUrl + '\'' +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", createTime=" + createTime +
                ", status=" + status +
                ", videoType='" + videoType + '\'' +
                '}';
    }
}