package com.video.platform.video.feign;

import com.video.platform.notification.entity.Notification;
import com.video.platform.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "video-notification", path = "/api/notification")
public interface NotificationFeign {
    
    @PostMapping("/send")
    Result send(@RequestBody Notification notification);
}
