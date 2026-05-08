package com.video.platform.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.video.platform.video.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
