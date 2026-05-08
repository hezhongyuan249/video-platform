package com.video.platform.video.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.platform.video.entity.Comment;
import com.video.platform.video.entity.CommentLike;
import com.video.platform.video.entity.Video;
import com.video.platform.video.mapper.CommentMapper;
import com.video.platform.video.mapper.CommentLikeMapper;
import com.video.platform.video.mapper.VideoMapper;
import com.video.platform.video.feign.NotificationFeign;
import com.video.platform.user.entity.User;
import com.video.platform.user.mapper.UserMapper;
import com.video.platform.common.result.Result;
import com.video.platform.notification.entity.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/comment")
public class CommentController {

    @Autowired
    private CommentMapper commentMapper;
    
    @Autowired
    private CommentLikeMapper commentLikeMapper;
    
    @Autowired
    private VideoMapper videoMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private NotificationFeign notificationFeign;

    @GetMapping("/list")
    public Result list(
            @RequestParam Long videoId,
            @RequestParam(required = false) Long currentUserId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        // 先查询顶级评论（parent_id is null）
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getVideoId, videoId)
               .isNull(Comment::getParentId)
               .orderByDesc(Comment::getCreateTime);
        
        Page<Comment> page = commentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        
        // 获取当前用户已点赞的评论ID集合
        Set<Long> likedCommentIds = new HashSet<>();
        if (currentUserId != null) {
            LambdaQueryWrapper<CommentLike> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(CommentLike::getUserId, currentUserId);
            List<CommentLike> likes = commentLikeMapper.selectList(likeWrapper);
            for (CommentLike like : likes) {
                likedCommentIds.add(like.getCommentId());
            }
        }
        
        // 补充用户信息
        for (Comment c : page.getRecords()) {
            User user = userMapper.selectById(c.getUserId());
            if (user != null) {
                c.setUsername(user.getUsername());
                c.setAvatar(user.getAvatar());
            }
            c.setLiked(likedCommentIds.contains(c.getId()));
            
            // 查询回复
            LambdaQueryWrapper<Comment> replyWrapper = new LambdaQueryWrapper<>();
            replyWrapper.eq(Comment::getParentId, c.getId())
                       .orderByAsc(Comment::getCreateTime);
            List<Comment> replies = commentMapper.selectList(replyWrapper);
            
            for (Comment reply : replies) {
                User replyUser = userMapper.selectById(reply.getUserId());
                if (replyUser != null) {
                    reply.setUsername(replyUser.getUsername());
                    reply.setAvatar(replyUser.getAvatar());
                }
                if (reply.getReplyToUserId() != null) {
                    User replyToUser = userMapper.selectById(reply.getReplyToUserId());
                    if (replyToUser != null) {
                        reply.setReplyToUsername(replyToUser.getUsername());
                    }
                }
                reply.setLiked(likedCommentIds.contains(reply.getId()));
            }
            c.setReplies(replies);
        }
        
        return Result.success(page);
    }

    @PostMapping("/add")
    public Result add(@RequestBody Comment comment) {
        if (comment.getUserId() == null || comment.getVideoId() == null || 
            comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            return Result.error("参数不完整");
        }
        
        comment.setLikeCount(0);
        comment.setCreateTime(new Date());
        commentMapper.insert(comment);
        
        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            comment.setUsername(user.getUsername());
            comment.setAvatar(user.getAvatar());
            
            // 发送通知
             try {
                 // 获取视频信息，获取作者ID
                 Video video = videoMapper.selectById(comment.getVideoId());
                if (video != null) {
                    Long receiverId = video.getUserId();
                    // 如果是回复评论，通知原评论作者；如果是新评论，通知视频作者
                    if (comment.getParentId() != null && comment.getReplyToUserId() != null) {
                        receiverId = comment.getReplyToUserId();
                        // 不通知自己
                        if (!receiverId.equals(comment.getUserId())) {
                            Notification notification = new Notification();
                            notification.setUserId(receiverId);
                            notification.setType("comment_reply");
                            notification.setContent(user.getUsername() + "回复了你的评论");
                            notification.setRelatedId(comment.getId());
                            notification.setVideoId(comment.getVideoId());
                            notificationFeign.send(notification);
                        }
                    } else if (!receiverId.equals(comment.getUserId())) {
                        // 新评论通知视频作者
                        Notification notification = new Notification();
                        notification.setUserId(receiverId);
                        notification.setType("comment");
                        notification.setContent(user.getUsername() + "评论了你的视频");
                        notification.setRelatedId(comment.getId());
                        notification.setVideoId(comment.getVideoId());
                        notificationFeign.send(notification);
                    }
                }
            } catch (Exception e) {
                // 通知发送失败不影响评论功能
            }
        }
        
        return Result.success(comment);
    }

    @PostMapping("/delete")
    public Result delete(@RequestParam Long id, @RequestParam Long userId) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            return Result.error("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            return Result.error("无权限删除");
        }
        
        // 删除评论及其所有回复
        commentMapper.deleteById(id);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getParentId, id);
        commentMapper.delete(wrapper);
        
        // 删除评论的点赞
        LambdaQueryWrapper<CommentLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(CommentLike::getCommentId, id);
        commentLikeMapper.delete(likeWrapper);
        
        return Result.success();
    }

    @PostMapping("/like")
    public Result like(@RequestParam Long commentId, @RequestParam Long userId) {
        if (commentId == null || userId == null) {
            return Result.error("参数不完整");
        }
        
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return Result.error("评论不存在");
        }
        
        // 检查是否已点赞
        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getCommentId, commentId)
               .eq(CommentLike::getUserId, userId);
        CommentLike existingLike = commentLikeMapper.selectOne(wrapper);
        
        if (existingLike != null) {
            // 取消点赞
            commentLikeMapper.deleteById(existingLike.getId());
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
            commentMapper.updateById(comment);
            return Result.success(comment.getLikeCount());
        } else {
            // 点赞
            CommentLike like = new CommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            like.setCreateTime(new Date());
            commentLikeMapper.insert(like);
            
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentMapper.updateById(comment);
            
            // 发送点赞通知
            try {
                User liker = userMapper.selectById(userId);
                if (liker != null && !comment.getUserId().equals(userId)) {
                    Notification notification = new Notification();
                    notification.setUserId(comment.getUserId());
                    notification.setType("comment_like");
                    notification.setContent(liker.getUsername() + "点赞了你的评论");
                    notification.setRelatedId(userId);
                    notification.setVideoId(comment.getVideoId());
                    notificationFeign.send(notification);
                }
            } catch (Exception e) {}
            
            return Result.success(comment.getLikeCount());
        }
    }
    
    // 简化的评论信息（用于通知）
    @GetMapping("/simple")
    public Result simple(@RequestParam Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            return Result.error("评论不存在");
        }
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", comment.getId());
        result.put("content", comment.getContent());
        result.put("videoId", comment.getVideoId());
        return Result.success(result);
    }
}
