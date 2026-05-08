package com.video.platform.notification.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.platform.notification.entity.Notification;
import com.video.platform.notification.feign.VideoFeign;
import com.video.platform.notification.mapper.NotificationMapper;
import com.video.platform.user.entity.User;
import com.video.platform.user.mapper.UserMapper;
import com.video.platform.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private VideoFeign videoFeign;

    @GetMapping("/list")
    public Result list(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreateTime);

        Page<Notification> page = notificationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        for (Notification n : page.getRecords()) {
            // 获取发送者信息
            if ("like".equals(n.getType()) || "favorite".equals(n.getType()) || "follow".equals(n.getType()) || "comment".equals(n.getType()) || "comment_reply".equals(n.getType()) || "comment_like".equals(n.getType())) {
                if (n.getRelatedId() != null) {
                    User sender = userMapper.selectById(n.getRelatedId());
                    if (sender != null) {
                        n.setSenderUsername(sender.getUsername());
                        n.setSenderAvatar(sender.getAvatar());
                    }
                }
            }
            
            // 获取评论内容和视频信息
            if ("comment".equals(n.getType()) || "comment_reply".equals(n.getType()) || "comment_like".equals(n.getType())) {
                try {
                    Result commentResult = videoFeign.getCommentSimple(n.getRelatedId());
                    System.out.println("=== Comment Result: code=" + (commentResult != null ? commentResult.getCode() : "null") + ", data=" + (commentResult != null ? commentResult.getData() : "null"));
                    if (commentResult != null && commentResult.getCode() == 200 && commentResult.getData() != null) {
                        Map<String, Object> commentInfo = (Map<String, Object>) commentResult.getData();
                        n.setRelatedCommentContent((String) commentInfo.get("content"));
                        // 获取视频ID并查询视频信息
                        Object videoIdObj = commentInfo.get("videoId");
                        Long videoIdForQuery = videoIdObj != null ? Long.valueOf(videoIdObj.toString()) : null;
                        System.out.println("=== videoIdForQuery: " + videoIdForQuery);
                        if (videoIdForQuery != null) {
                            Result videoResult = videoFeign.getVideoSimple(videoIdForQuery);
                            if (videoResult != null && videoResult.getCode() == 200 && videoResult.getData() != null) {
                                Map<String, Object> videoInfo = (Map<String, Object>) videoResult.getData();
                                n.setRelatedVideoTitle((String) videoInfo.get("title"));
                                n.setRelatedVideoCover((String) videoInfo.get("coverUrl"));
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("=== Error: " + e.getMessage());
                }
            }
            
            // 获取点赞和收藏的视频信息
            if ("like".equals(n.getType()) || "favorite".equals(n.getType())) {
                try {
                    Result videoResult = videoFeign.getVideoSimple(n.getRelatedId());
                    if (videoResult != null && videoResult.getCode() == 200 && videoResult.getData() != null) {
                        Map<String, Object> videoInfo = (Map<String, Object>) videoResult.getData();
                        n.setRelatedVideoTitle((String) videoInfo.get("title"));
                        n.setRelatedVideoCover((String) videoInfo.get("coverUrl"));
                    }
                } catch (Exception e) {}
            }
            
            // 评论点赞用videoId获取视频信息
            if ("comment_like".equals(n.getType())) {
                try {
                    if (n.getVideoId() != null) {
                        Result videoResult = videoFeign.getVideoSimple(n.getVideoId());
                        if (videoResult != null && videoResult.getCode() == 200 && videoResult.getData() != null) {
                            Map<String, Object> videoInfo = (Map<String, Object>) videoResult.getData();
                            n.setRelatedVideoTitle((String) videoInfo.get("title"));
                            n.setRelatedVideoCover((String) videoInfo.get("coverUrl"));
                        }
                    }
                } catch (Exception e) {}
            }
        }

        return Result.success(page);
    }

    @GetMapping("/unreadCount")
    public Result unreadCount(@RequestParam Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0);
        long count = notificationMapper.selectCount(wrapper);
        return Result.success(count);
    }

    @PostMapping("/markRead")
    public Result markRead(@RequestParam Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0);

        Notification update = new Notification();
        update.setIsRead(1);
        notificationMapper.update(update, wrapper);
        return Result.success();
    }

    @PostMapping("/markReadOne")
    public Result markReadOne(@RequestParam Long id) {
        Notification update = new Notification();
        update.setId(id);
        update.setIsRead(1);
        notificationMapper.updateById(update);
        return Result.success();
    }

    @PostMapping("/deleteBatch")
    public Result deleteBatch(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("删除的ID不能为空");
        }
        notificationMapper.deleteBatchIds(ids);
        return Result.success();
    }

    @PostMapping("/send")
    public Result send(@RequestBody Notification notification) {
        notification.setIsRead(0);
        notification.setCreateTime(new Date());
        notificationMapper.insert(notification);
        return Result.success();
    }
}
