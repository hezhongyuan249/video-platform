package com.video.platform.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.util.Date;

@TableName("tb_notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private String content;
    private Long relatedId;
    private Long videoId;
    private Integer isRead = 0;
    
    @TableField("create_time")
    private Date createTime;
    
    @TableField(exist = false)
    private String senderUsername;
    @TableField(exist = false)
    private String senderAvatar;
    @TableField(exist = false)
    private String relatedVideoCover;
    @TableField(exist = false)
    private String relatedVideoTitle;
    @TableField(exist = false)
    private String relatedCommentContent;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getRelatedId() { return relatedId; }
    public void setRelatedId(Long relatedId) { this.relatedId = relatedId; }
    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }
    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }
    public String getRelatedVideoCover() { return relatedVideoCover; }
    public void setRelatedVideoCover(String relatedVideoCover) { this.relatedVideoCover = relatedVideoCover; }
    public String getRelatedVideoTitle() { return relatedVideoTitle; }
    public void setRelatedVideoTitle(String relatedVideoTitle) { this.relatedVideoTitle = relatedVideoTitle; }
    public String getRelatedCommentContent() { return relatedCommentContent; }
    public void setRelatedCommentContent(String relatedCommentContent) { this.relatedCommentContent = relatedCommentContent; }
}
