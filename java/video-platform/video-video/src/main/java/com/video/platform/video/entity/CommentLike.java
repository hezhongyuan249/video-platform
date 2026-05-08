package com.video.platform.video.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("tb_comment_like")
public class CommentLike {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long commentId;
    private Long userId;
    
    @TableField("create_time")
    private Date createTime;
}
