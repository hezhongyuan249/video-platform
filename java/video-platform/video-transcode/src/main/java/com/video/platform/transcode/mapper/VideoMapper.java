package com.video.platform.transcode.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.video.platform.transcode.entity.Video;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VideoMapper extends BaseMapper<Video> {
}
