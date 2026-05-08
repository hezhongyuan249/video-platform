package com.video.platform.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.video.platform.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}