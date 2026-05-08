package com.video.platform.video.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
@TableName("tb_comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long videoId;
    private Long userId;
    private String content;
    private Long parentId;
    private Long replyToUserId;
    private Integer likeCount = 0;
    
    @TableField("create_time")
    private Date createTime;
    
    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String avatar;
    @TableField(exist = false)
    private String replyToUsername;
    @TableField(exist = false)
    private Boolean liked = false;
    @TableField(exist = false)
    private List<Comment> replies;
}
