package com.video.platform.video.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.platform.common.result.Result;
import com.video.platform.video.entity.Video;
import com.video.platform.video.entity.VideoLike;
import com.video.platform.video.entity.VideoFavorite;
import com.video.platform.video.entity.UserFollow;
import com.video.platform.video.entity.Category;
import com.video.platform.notification.entity.Notification;
import com.video.platform.user.entity.User;
import com.video.platform.video.mapper.VideoMapper;
import com.video.platform.video.mapper.VideoLikeMapper;
import com.video.platform.video.mapper.VideoFavoriteMapper;
import com.video.platform.video.mapper.UserFollowMapper;
import com.video.platform.video.mapper.CategoryMapper;
import com.video.platform.user.mapper.UserMapper;
import com.video.platform.video.feign.NotificationFeign;
import com.video.platform.video.feign.TranscodeFeignClient;
import com.video.platform.video.util.FFmpegUtil;
import com.video.platform.video.util.TranscodeProgress;
import org.springframework.util.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/video")
public class VideoController {

    @Resource
    private VideoMapper videoMapper;
    @Resource
    private VideoLikeMapper videoLikeMapper;
    @Resource
    private VideoFavoriteMapper videoFavoriteMapper;
    @Resource
    private UserFollowMapper userFollowMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private CategoryMapper categoryMapper;
    @Resource
    private NotificationFeign notificationFeign;
    @Resource
    private FFmpegUtil ffmpegUtil;
    @Resource
    private TranscodeFeignClient transcodeFeignClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String VIDEO_PATH = "C:/test/videos/";
    private static final String COVER_PATH = "C:/test/covers/";
    private static final String VIDEO_URL_PREFIX = "http://localhost:8088/videos/";
    private static final String COVER_URL_PREFIX = "http://localhost:8088/covers/";

    private void sendNotification(Long userId, String type, String content, Long relatedId) {
        if (userId == null) return;
        try {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType(type);
            notification.setContent(content);
            notification.setRelatedId(relatedId);
            notificationFeign.send(notification);
        } catch (Exception e) {
        }
    }

    // 上传
    @PostMapping("/upload")
    public Result uploadVideo(
            @RequestParam("title") String title,
            @RequestParam("intro") String intro,
            @RequestParam("video") MultipartFile videoFile,
            @RequestParam("cover") MultipartFile coverFile,
            @RequestParam("userId") Long userId,
            @RequestParam("username") String username,
            @RequestParam("videoType") String videoType
    ) {
        System.out.println("收到上传请求：" + title + ", userId=" + userId + ", videoType=" + videoType);
        if (!StringUtils.hasText(title)) return Result.error("标题不能为空");
        if (!StringUtils.hasText(videoType)) return Result.error("请选择视频分类");
        try {
            System.out.println("视频文件：" + videoFile.getOriginalFilename() + ", 大小：" + videoFile.getSize());
            System.out.println("封面文件：" + coverFile.getOriginalFilename() + ", 大小：" + coverFile.getSize());
            
            new File(VIDEO_PATH).mkdirs();
            new File(COVER_PATH).mkdirs();
            
            System.out.println("存储路径：" + VIDEO_PATH + ", " + COVER_PATH);
            
            String vidName = UUID.randomUUID() + "_" + videoFile.getOriginalFilename();
            String covName = UUID.randomUUID() + "_" + coverFile.getOriginalFilename();
            
            System.out.println("文件名：" + vidName + ", " + covName);
            
            videoFile.transferTo(new File(VIDEO_PATH + vidName));
            coverFile.transferTo(new File(COVER_PATH + covName));
            
            System.out.println("文件保存成功");

            String inputPath = VIDEO_PATH + vidName;
            String baseName = vidName.substring(0, vidName.lastIndexOf("."));
            
            Integer duration = ffmpegUtil.getVideoDuration(inputPath);
            System.out.println("视频时长: " + duration + "秒");

            Video v = new Video();
            v.setTitle(title);
            v.setIntro(intro);
            v.setVideoUrl(VIDEO_URL_PREFIX + vidName);
            v.setCoverUrl(COVER_URL_PREFIX + covName);
            v.setDuration(duration);
            v.setUserId(userId);
            v.setUsername(username);
            v.setCreateTime(new Date());
            v.setStatus(0);
            v.setVideoType(videoType);
            v.setReviewStatus(0);
            
            System.out.println("准备插入数据库：" + v.toString());
            
            int result = videoMapper.insert(v);
            System.out.println("插入结果：" + result);
            
            if (result > 0) {
                final Long videoId = v.getId();
                final String finalInputPath = inputPath;
                final String finalBaseName = baseName;
                
                transcodeFeignClient.executeTranscode(videoId, finalInputPath, finalBaseName);
                
                sendNotification(userId, "video_upload", "您的视频《" + title + "》已提交，待审核", v.getId());
                System.out.println("上传成功");
                Map<String, Object> uploadResult = new HashMap<>();
                uploadResult.put("videoId", v.getId());
                return Result.success(uploadResult);
            } else {
                System.out.println("插入数据库失败");
                return Result.error("插入数据库失败");
            }
        } catch (IOException e) {
            System.out.println("上传失败：" + e.getMessage());
            e.printStackTrace();
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    // 获取用户自己的视频列表
    @GetMapping("/myList")
    public Result getMyList(@RequestParam Long userId) {
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Video::getUserId, userId).orderByDesc(Video::getCreateTime);
        List<Video> videos = videoMapper.selectList(wrapper);
        
        // 填充作者信息、点赞数、收藏数
        User author = userMapper.selectById(userId);
        if (author != null) {
            for (Video v : videos) {
                v.setUsername(author.getUsername());
                v.setUserAvatar(author.getAvatar());
                
                // 填充点赞数
                LambdaQueryWrapper<VideoLike> likeWrapper = new LambdaQueryWrapper<>();
                likeWrapper.eq(VideoLike::getVideoId, v.getId());
                Long likeCount = videoLikeMapper.selectCount(likeWrapper);
                v.setLikes(likeCount.intValue());
                
                // 填充收藏数
                LambdaQueryWrapper<VideoFavorite> favWrapper = new LambdaQueryWrapper<>();
                favWrapper.eq(VideoFavorite::getVideoId, v.getId());
                Long favCount = videoFavoriteMapper.selectCount(favWrapper);
                v.setFavorites(favCount.intValue());
            }
        }
        
        return Result.success(videos);
    }
    
    // 获取用户收藏的视频列表
    @GetMapping("/favoritedList")
    public Result getFavoritedList(@RequestParam Long userId) {
        LambdaQueryWrapper<VideoFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoFavorite::getUserId, userId)
               .orderByDesc(VideoFavorite::getCreateTime);
        List<VideoFavorite> favorites = videoFavoriteMapper.selectList(wrapper);
        
        // 获取视频信息
        List<Video> videos = new ArrayList<>();
        for (VideoFavorite f : favorites) {
            Video v = videoMapper.selectById(f.getVideoId());
            if (v != null) {
                // 填充作者信息
                User author = userMapper.selectById(v.getUserId());
                if (author != null) {
                    v.setUsername(author.getUsername());
                    v.setUserAvatar(author.getAvatar());
                }
                // 填充点赞数
                LambdaQueryWrapper<VideoLike> likeWrapper = new LambdaQueryWrapper<>();
                likeWrapper.eq(VideoLike::getVideoId, v.getId());
                Long likeCount = videoLikeMapper.selectCount(likeWrapper);
                v.setLikes(likeCount.intValue());
                
                // 填充收藏数
                LambdaQueryWrapper<VideoFavorite> favWrapper = new LambdaQueryWrapper<>();
                favWrapper.eq(VideoFavorite::getVideoId, v.getId());
                Long favCount = videoFavoriteMapper.selectCount(favWrapper);
                v.setFavorites(favCount.intValue());
                
                videos.add(v);
            }
        }
        
        return Result.success(videos);
    }
    
    // 获取用户关注的作者列表
    @GetMapping("/myFollowingList")
    public Result getMyFollowingList(@RequestParam Long userId) {
        // 获取用户关注的所有作者
        LambdaQueryWrapper<UserFollow> followWrapper = new LambdaQueryWrapper<>();
        followWrapper.eq(UserFollow::getFollowerId, userId);
        List<UserFollow> follows = userFollowMapper.selectList(followWrapper);
        
        if (follows.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        // 获取所有关注作者的详细信息
        List<Long> followingIds = follows.stream()
            .map(UserFollow::getFollowingId)
            .collect(Collectors.toList());
        
        List<User> authors = userMapper.selectBatchIds(followingIds);
        return Result.success(authors);
    }
    
    // 获取用户关注的作者的视频列表
    @GetMapping("/followingVideos")
    public Result getFollowingVideos(@RequestParam Long userId, @RequestParam(required = false) Long authorId) {
        // 获取用户关注的所有作者
        LambdaQueryWrapper<UserFollow> followWrapper = new LambdaQueryWrapper<>();
        followWrapper.eq(UserFollow::getFollowerId, userId);
        List<UserFollow> follows = userFollowMapper.selectList(followWrapper);
        
        if (follows.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        // 获取所有关注作者的ID
        List<Long> followingIds = follows.stream()
            .map(UserFollow::getFollowingId)
            .collect(Collectors.toList());
        
        // 如果指定了作者ID，则只获取该作者的视频
        if (authorId != null) {
            followingIds = followingIds.stream()
                .filter(id -> id.equals(authorId))
                .collect(Collectors.toList());
        }
        
        if (followingIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        // 获取这些作者已审核通过的视频
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Video::getUserId, followingIds)
               .eq(Video::getReviewStatus, 1)
               .eq(Video::getStatus, 0)
               .orderByDesc(Video::getCreateTime);
        List<Video> videos = videoMapper.selectList(wrapper);
        
        // 填充作者信息、点赞数、收藏数
        for (Video v : videos) {
            if (v.getUserId() != null) {
                User author = userMapper.selectById(v.getUserId());
                if (author != null) {
                    v.setUsername(author.getUsername());
                    v.setUserAvatar(author.getAvatar());
                }
            }
            // 填充点赞数
            LambdaQueryWrapper<VideoLike> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(VideoLike::getVideoId, v.getId());
            Long likeCount = videoLikeMapper.selectCount(likeWrapper);
            v.setLikes(likeCount.intValue());
            
            // 填充收藏数
            LambdaQueryWrapper<VideoFavorite> favWrapper = new LambdaQueryWrapper<>();
            favWrapper.eq(VideoFavorite::getVideoId, v.getId());
            Long favCount = videoFavoriteMapper.selectCount(favWrapper);
            v.setFavorites(favCount.intValue());
        }
        
        return Result.success(videos);
    }

    // 用户删除自己的视频
    @PostMapping("/delete")
    public Result deleteVideo(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        Long userId = Long.valueOf(params.get("userId").toString());
        
        Video video = videoMapper.selectById(id);
        if (video == null) {
            return Result.error("视频不存在");
        }
        if (!video.getUserId().equals(userId)) {
            return Result.error("无权限删除");
        }
        
        // 删除本地文件
        if (video.getVideoUrl() != null) {
            String videoFilename = video.getVideoUrl().replace(VIDEO_URL_PREFIX, "");
            File videoFile = new File(VIDEO_PATH + videoFilename);
            if (videoFile.exists()) {
                videoFile.delete();
            }
        }
        if (video.getCoverUrl() != null) {
            String coverFilename = video.getCoverUrl().replace(COVER_URL_PREFIX, "");
            File coverFile = new File(COVER_PATH + coverFilename);
            if (coverFile.exists()) {
                coverFile.delete();
            }
        }
        
        videoMapper.deleteById(id);
        return Result.success();
    }

    // 编辑
    @PostMapping("/edit")
    public Result editVideo(
            @RequestParam Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String intro,
            @RequestParam(required = false) MultipartFile coverFile,
            @RequestParam(required = false) String videoType,
            @RequestParam(required = false) MultipartFile videoFile,
            @RequestParam Long userId
    ) {
        Video v = videoMapper.selectById(id);
        if (v == null) return Result.error("视频不存在");
        if (!v.getUserId().equals(userId)) return Result.error("无权限");

        // 保存到待审核字段，不直接修改原数据
        LambdaUpdateWrapper<Video> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Video::getId, id);
        if (StringUtils.hasText(title)) wrapper.set(Video::getPendingTitle, title);
        if (StringUtils.hasText(intro)) wrapper.set(Video::getPendingIntro, intro);
        if (StringUtils.hasText(videoType)) wrapper.set(Video::getPendingVideoType, videoType);
        
        // 处理新视频文件
        if (videoFile != null && !videoFile.isEmpty()) {
            try {
                String videoName = UUID.randomUUID() + "_" + videoFile.getOriginalFilename();
                videoFile.transferTo(new File(VIDEO_PATH + videoName));
                wrapper.set(Video::getPendingVideoUrl, VIDEO_URL_PREFIX + videoName);
            } catch (Exception e) {
                return Result.error("视频上传失败");
            }
        }
        
        // 标记为待审核，进入审核列表
        wrapper.set(Video::getReviewStatus, 0);
        wrapper.set(Video::getHasPendingEdit, true);

        if (coverFile != null && !coverFile.isEmpty()) {
            try {
                String covName = UUID.randomUUID() + "_" + coverFile.getOriginalFilename();
                coverFile.transferTo(new File(COVER_PATH + covName));
                wrapper.set(Video::getPendingCoverUrl, COVER_URL_PREFIX + covName);
            } catch (Exception e) {
                return Result.error("封面上传失败");
            }
        }
        videoMapper.update(null, wrapper);
        return Result.success();
    }

    // 首页列表 - 显示审核通过的视频，编辑后待审核的也显示（显示旧数据）
    @GetMapping("/list")
    public Result getVideoList() {
        // 显示已审核通过的视频，或者之前已审核通过现在编辑后待审核的视频
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Video::getStatus, 0)
               .orderByDesc(Video::getCreateTime);
        List<Video> videos = videoMapper.selectList(wrapper);
        
        // 过滤：显示已审核通过(reviewStatus=1)的视频，以及有待审核修改的视频
        videos = videos.stream()
            .filter(v -> v.getReviewStatus() == 1 || (v.getHasPendingEdit() != null && v.getHasPendingEdit()))
            .collect(Collectors.toList());
        
        // 填充作者信息、点赞数、收藏数
        for (Video v : videos) {
            if (v.getUserId() != null) {
                User author = userMapper.selectById(v.getUserId());
                if (author != null) {
                    v.setUsername(author.getUsername());
                    v.setUserAvatar(author.getAvatar());
                }
            }
            // 填充点赞数
            LambdaQueryWrapper<VideoLike> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(VideoLike::getVideoId, v.getId());
            Long likeCount = videoLikeMapper.selectCount(likeWrapper);
            v.setLikes(likeCount.intValue());
            
            // 填充收藏数
            LambdaQueryWrapper<VideoFavorite> favWrapper = new LambdaQueryWrapper<>();
            favWrapper.eq(VideoFavorite::getVideoId, v.getId());
            Long favCount = videoFavoriteMapper.selectCount(favWrapper);
            v.setFavorites(favCount.intValue());
        }
        
        return Result.success(videos);
    }

    // 管理员视频列表
    @GetMapping("/admin/list")
    public Result adminList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String title
    ) {
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(title)) wrapper.like(Video::getTitle, title);
        wrapper.orderByDesc(Video::getCreateTime);
        Page<Video> page = videoMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        
        // 填充作者信息
        for (Video v : page.getRecords()) {
            if (v.getUserId() != null) {
                User author = userMapper.selectById(v.getUserId());
                if (author != null) {
                    v.setUsername(author.getUsername());
                    v.setUserAvatar(author.getAvatar());
                }
            }
        }
        
        return Result.success(page);
    }

    // 审核列表 - 只显示待审核的视频
    @GetMapping("/admin/review/list")
    public Result reviewList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String title
    ) {
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Video::getReviewStatus, 0);  // 只显示待审核的
        if (StringUtils.hasText(title)) {
            wrapper.like(Video::getTitle, title);
        }
        wrapper.orderByDesc(Video::getCreateTime);
        Page<Video> page = videoMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        
        // 填充作者信息，如果有待审核的修改则显示新数据
        for (Video v : page.getRecords()) {
            // 如果有待审核的修改，临时替换显示新数据
            if (v.getPendingTitle() != null) {
                v.setTitle(v.getPendingTitle());
            }
            if (v.getPendingIntro() != null) {
                v.setIntro(v.getPendingIntro());
            }
            if (v.getPendingCoverUrl() != null) {
                v.setCoverUrl(v.getPendingCoverUrl());
            }
            if (v.getPendingVideoType() != null) {
                v.setVideoType(v.getPendingVideoType());
            }
            if (v.getPendingVideoUrl() != null) {
                v.setVideoUrl(v.getPendingVideoUrl());
            }
            
            // 获取作者信息
            if (v.getUserId() != null) {
                User author = userMapper.selectById(v.getUserId());
                if (author != null) {
                    v.setUsername(author.getUsername());
                    v.setUserAvatar(author.getAvatar());
                }
            }
        }
        
        return Result.success(page);
    }

    // 审核通过
    @PostMapping("/admin/review/pass")
    public Result pass(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        Long reviewerId = Long.valueOf(params.get("reviewerId").toString());
        String reviewerName = params.get("reviewerName").toString();
        
        // 检查是否已被审核
        Video existingVideo = videoMapper.selectById(id);
        if (existingVideo != null && existingVideo.getReviewStatus() != 0) {
            return Result.error("该视频已被审核");
        }
        
        // 如果有待审核的修改，覆盖原数据
        LambdaUpdateWrapper<Video> w = new LambdaUpdateWrapper<>();
        w.eq(Video::getId, id)
         .set(Video::getReviewStatus, 1)  // 审核通过
         .set(Video::getReviewTime, new Date())
         .set(Video::getReviewerId, reviewerId)
         .set(Video::getReviewerName, reviewerName)
         .set(Video::getStatus, 0);
        
        // 待审核数据覆盖原数据
        if (existingVideo != null) {
            // 直接更新Video对象
            if (existingVideo.getPendingTitle() != null) {
                existingVideo.setTitle(existingVideo.getPendingTitle());
            }
            if (existingVideo.getPendingIntro() != null) {
                existingVideo.setIntro(existingVideo.getPendingIntro());
            }
            if (existingVideo.getPendingCoverUrl() != null) {
                existingVideo.setCoverUrl(existingVideo.getPendingCoverUrl());
            }
            if (existingVideo.getPendingVideoType() != null) {
                existingVideo.setVideoType(existingVideo.getPendingVideoType());
            }
            if (existingVideo.getPendingVideoUrl() != null) {
                existingVideo.setVideoUrl(existingVideo.getPendingVideoUrl());
            }
            // 清除待审核数据
            existingVideo.setPendingTitle(null);
            existingVideo.setPendingIntro(null);
            existingVideo.setPendingCoverUrl(null);
            existingVideo.setPendingVideoType(null);
            existingVideo.setPendingVideoUrl(null);
            existingVideo.setHasPendingEdit(null);
            existingVideo.setReviewStatus(1);
            existingVideo.setReviewTime(new Date());
            existingVideo.setReviewerId(reviewerId);
            existingVideo.setReviewerName(reviewerName);
            existingVideo.setStatus(0);
            videoMapper.updateById(existingVideo);
        } else {
            videoMapper.update(null, w);
        }
        
        // 发送通知给视频作者
        Video video = videoMapper.selectById(id);
        if (video != null && video.getUserId() != null) {
            sendNotification(video.getUserId(), "video_review", "您的视频《" + video.getTitle() + "》审核通过", id);
        }
        
        return Result.success();
    }

    // 审核驳回
    @PostMapping("/admin/review/reject")
    public Result reject(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        Long reviewerId = Long.valueOf(params.get("reviewerId").toString());
        String reviewerName = params.get("reviewerName").toString();
        String reason = params.get("reason").toString();
        
        // 获取视频信息
        Video video = videoMapper.selectById(id);
        if (video == null) {
            return Result.error("视频不存在");
        }
        
        // 检查是否已被审核（只能对待审核的视频进行审核）
        if (video.getReviewStatus() != 0) {
            return Result.error("该视频已被审核");
        }
        
        // 判断是否有待审核的修改
        if (video.getHasPendingEdit() != null && video.getHasPendingEdit()) {
            // 有待审核的修改：驳回修改，保持原视频不变
            // 清除待审核数据
            LambdaUpdateWrapper<Video> w = new LambdaUpdateWrapper<>();
            w.eq(Video::getId, id)
             .set(Video::getReviewStatus, 1)  // 保持原视频为审核通过
             .set(Video::getReviewTime, new Date())
             .set(Video::getReviewerId, reviewerId)
             .set(Video::getReviewerName, reviewerName)
             .set(Video::getRejectReason, reason)
             .set(Video::getPendingTitle, null)
             .set(Video::getPendingIntro, null)
             .set(Video::getPendingCoverUrl, null)
             .set(Video::getPendingVideoType, null)
             .set(Video::getPendingVideoUrl, null)
             .set(Video::getHasPendingEdit, null);
            videoMapper.update(null, w);
            
            // 发送通知
            sendNotification(video.getUserId(), "video_review", 
                "您修改的视频《" + video.getTitle() + "》审核未通过，原因：" + reason, id);
        } else {
            // 原始视频待审核：直接驳回
            LambdaUpdateWrapper<Video> w = new LambdaUpdateWrapper<>();
            w.eq(Video::getId, id)
             .set(Video::getReviewStatus, 2)  // 审核拒绝
             .set(Video::getReviewTime, new Date())
             .set(Video::getReviewerId, reviewerId)
             .set(Video::getReviewerName, reviewerName)
             .set(Video::getRejectReason, reason);
            videoMapper.update(null, w);
            
            // 发送通知
            sendNotification(video.getUserId(), "video_review", 
                "您的视频《" + video.getTitle() + "》审核未通过，原因：" + reason, id);
        }
        
        return Result.success();
    }

    // 状态切换（修复版）
    @PostMapping("/admin/changeStatus")
    public Result changeStatus(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        Integer status = (Integer) params.get("status");
        if (status != 0 && status != 1) return Result.error("状态错误");

        LambdaUpdateWrapper<Video> w = new LambdaUpdateWrapper<>();
        w.eq(Video::getId, id).set(Video::getStatus, status);
        videoMapper.update(null, w);
        return Result.success();
    }
    
    // 管理员删除视频
    @PostMapping("/admin/delete")
    public Result adminDelete(@RequestBody Map<String, Long> params) {
        Long id = params.get("id");
        
        Video video = videoMapper.selectById(id);
        if (video == null) {
            return Result.error("视频不存在");
        }
        
        // 删除本地文件
        if (video.getVideoUrl() != null) {
            String videoFilename = video.getVideoUrl().replace(VIDEO_URL_PREFIX, "");
            File videoFile = new File(VIDEO_PATH + videoFilename);
            if (videoFile.exists()) {
                videoFile.delete();
            }
        }
        if (video.getCoverUrl() != null) {
            String coverFilename = video.getCoverUrl().replace(COVER_URL_PREFIX, "");
            File coverFile = new File(COVER_PATH + coverFilename);
            if (coverFile.exists()) {
                coverFile.delete();
            }
        }
        
        videoMapper.deleteById(id);
        return Result.success();
    }

    // ===================== 观看历史 =====================
    @PostMapping("/history/add")
    public Result addHistory(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        Long videoId = Long.valueOf(params.get("videoId").toString());
        
        String key = "watch_history:" + userId;
        String newValue = videoId + ":" + System.currentTimeMillis();
        
        // 先移除该视频的旧记录（如果存在）
        List<String> oldList = stringRedisTemplate.opsForList().range(key, 0, -1);
        if (oldList != null) {
            for (String item : oldList) {
                if (item.startsWith(videoId + ":")) {
                    stringRedisTemplate.opsForList().remove(key, 1, item);
                    break;
                }
            }
        }
        
        // 将观看记录添加到Redis列表（左侧插入，最新的在最前面）
        stringRedisTemplate.opsForList().leftPush(key, newValue);
        
        // 限制历史记录数量，最多保留100条
        stringRedisTemplate.opsForList().trim(key, 0, 99);
        
        return Result.success();
    }

    @GetMapping("/history/list")
    public Result getHistoryList(@RequestParam Long userId) {
        String key = "watch_history:" + userId;
        List<String> historyList = stringRedisTemplate.opsForList().range(key, 0, 99);
        
        if (historyList == null || historyList.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (String item : historyList) {
            String[] parts = item.split(":");
            if (parts.length >= 2) {
                Long videoId = Long.parseLong(parts[0]);
                Long watchedAt = Long.parseLong(parts[1]);
                
                Video video = videoMapper.selectById(videoId);
                if (video != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", video.getId());
                    map.put("title", video.getTitle());
                    map.put("coverUrl", video.getCoverUrl());
                    map.put("videoUrl", video.getVideoUrl());
                    map.put("username", video.getUsername());
                    map.put("views", video.getViews());
                    map.put("watchedAt", new Date(watchedAt));
                    map.put("duration", video.getDuration());
                    
                    // 获取播放进度
                    String progressKey = "watch_progress:" + userId + ":" + videoId;
                    String progressStr = stringRedisTemplate.opsForValue().get(progressKey);
                    if (progressStr != null) {
                        map.put("progress", Integer.parseInt(progressStr));
                    } else {
                        map.put("progress", 0);
                    }
                    
                    result.add(map);
                }
            }
        }
        
        return Result.success(result);
    }

    @PostMapping("/history/clear")
    public Result clearHistory(@RequestParam Long userId) {
        String key = "watch_history:" + userId;
        stringRedisTemplate.delete(key);
        // 同时清除所有播放进度
        Set<String> progressKeys = stringRedisTemplate.keys("watch_progress:" + userId + ":*");
        if (progressKeys != null && !progressKeys.isEmpty()) {
            stringRedisTemplate.delete(progressKeys);
        }
        return Result.success();
    }

    // 删除单条观看记录
    @PostMapping("/history/delete")
    public Result deleteHistory(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        Long videoId = Long.valueOf(params.get("videoId").toString());
        
        String key = "watch_history:" + userId;
        List<String> historyList = stringRedisTemplate.opsForList().range(key, 0, -1);
        
        if (historyList != null) {
            for (String item : historyList) {
                if (item.startsWith(videoId + ":")) {
                    stringRedisTemplate.opsForList().remove(key, 1, item);
                    break;
                }
            }
        }
        
        // 清除该视频的播放进度
        String progressKey = "watch_progress:" + userId + ":" + videoId;
        stringRedisTemplate.delete(progressKey);
        
        return Result.success();
    }

    // 批量删除观看记录
    @PostMapping("/history/deleteBatch")
    public Result deleteHistoryBatch(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        
        // 处理videoIds，可能是List或String
        List<Long> videoIds = new ArrayList<>();
        Object videoIdsObj = params.get("videoIds");
        if (videoIdsObj instanceof List) {
            List<?> videoIdsList = (List<?>) videoIdsObj;
            for (Object id : videoIdsList) {
                if (id instanceof Number) {
                    videoIds.add(((Number) id).longValue());
                } else {
                    videoIds.add(Long.valueOf(id.toString()));
                }
            }
        }
        
        String key = "watch_history:" + userId;
        
        for (Long videoId : videoIds) {
            List<String> historyList = stringRedisTemplate.opsForList().range(key, 0, -1);
            if (historyList != null) {
                for (String item : historyList) {
                    if (item.startsWith(videoId + ":")) {
                        stringRedisTemplate.opsForList().remove(key, 1, item);
                        break;
                    }
                }
            }
            // 清除播放进度
            String progressKey = "watch_progress:" + userId + ":" + videoId;
            stringRedisTemplate.delete(progressKey);
        }
        
        return Result.success();
    }

    @PostMapping("/history/progress")
    public Result saveProgress(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        Long videoId = Long.valueOf(params.get("videoId").toString());
        Integer progress = Integer.valueOf(params.get("progress").toString());
        
        System.out.println("保存播放进度: userId=" + userId + ", videoId=" + videoId + ", progress=" + progress);
        
        String key = "watch_progress:" + userId + ":" + videoId;
        stringRedisTemplate.opsForValue().set(key, progress.toString());
        
        return Result.success();
    }

    @GetMapping("/history/progress")
    public Result getProgress(@RequestParam Long userId, @RequestParam Long videoId) {
        String key = "watch_progress:" + userId + ":" + videoId;
        String progress = stringRedisTemplate.opsForValue().get(key);
        
        System.out.println("获取播放进度: userId=" + userId + ", videoId=" + videoId + ", progress=" + progress);
        
        if (progress != null) {
            return Result.success(Integer.parseInt(progress));
        }
        return Result.success(0);
    }

    // ===================== 新增：根据ID查询视频详情（修复播放404） =====================
    @GetMapping("/getById")
    public Result getById(@RequestParam Long id, @RequestParam(required = false) Long userId) {
        Video video = videoMapper.selectById(id);
        if (video == null) {
            return Result.error("视频不存在");
        }
        
        // 查询作者信息
        if (video.getUserId() != null) {
            User author = userMapper.selectById(video.getUserId());
            if (author != null) {
                video.setUsername(author.getUsername());
                video.setUserAvatar(author.getAvatar());
                video.setUserDescription(author.getDescription());
            }
            
            // 获取作者统计数据
            LambdaQueryWrapper<Video> videoWrapper = new LambdaQueryWrapper<>();
            videoWrapper.eq(Video::getUserId, video.getUserId());
            long videosCount = videoMapper.selectCount(videoWrapper);
            
            List<Video> authorVideos = videoMapper.selectList(videoWrapper);
            int totalLikes = authorVideos.stream().mapToInt(v -> v.getLikes() != null ? v.getLikes() : 0).sum();
            
            LambdaQueryWrapper<VideoFavorite> favWrapper = new LambdaQueryWrapper<>();
            favWrapper.eq(VideoFavorite::getUserId, video.getUserId());
            long favoritesCount = videoFavoriteMapper.selectCount(favWrapper);
            
            LambdaQueryWrapper<UserFollow> followerWrapper = new LambdaQueryWrapper<>();
            followerWrapper.eq(UserFollow::getFollowingId, video.getUserId());
            long followersCount = userFollowMapper.selectCount(followerWrapper);
            
            video.setVideosCount((int) videosCount);
            video.setLikesCount(totalLikes);
            video.setFavoritesCount((int) favoritesCount);
            video.setFollowersCount((int) followersCount);
            
            // 检查当前用户是否关注了作者
            if (userId != null) {
                LambdaQueryWrapper<UserFollow> followWrapper = new LambdaQueryWrapper<>();
                followWrapper.eq(UserFollow::getFollowerId, userId)
                           .eq(UserFollow::getFollowingId, video.getUserId());
                boolean isFollowing = userFollowMapper.selectCount(followWrapper) > 0;
                video.setFollowing(isFollowing);
            }
        }
        
        // 增加播放量
        try {
            LambdaUpdateWrapper<Video> viewWrapper = new LambdaUpdateWrapper<>();
            viewWrapper.eq(Video::getId, id)
                      .setSql("views = views + 1");
            videoMapper.update(null, viewWrapper);
        } catch (Exception e) {
        }
        
        // 填充分类名称
        if (video.getVideoType() != null) {
            LambdaQueryWrapper<Category> categoryWrapper = new LambdaQueryWrapper<>();
            // videoType 存储的是 category 的 id 值
            try {
                long idValue = Long.parseLong(video.getVideoType().toString());
                categoryWrapper.eq(Category::getId, idValue);
            } catch (NumberFormatException e) {
                // 如果不是数字，尝试用 value 匹配
                categoryWrapper.eq(Category::getValue, video.getVideoType());
            }
            Category category = categoryMapper.selectOne(categoryWrapper);
            System.out.println("查询到的分类: " + category);
            if (category != null) {
                video.setCategoryName(category.getName());
                System.out.println("设置的分类名称: " + category.getName());
            }
        } else {
            System.out.println("视频类型为空");
        }
        
        // 检查视频审核状态
        if (video.getReviewStatus() == null || video.getReviewStatus() != 1) {
            return Result.error("视频待审核或已下架");
        }
        
        return Result.success(video);
    }
    
    // 简化的视频信息（用于通知）
    @GetMapping("/simple")
    public Result simple(@RequestParam Long id) {
        Video video = videoMapper.selectById(id);
        if (video == null) {
            return Result.error("视频不存在");
        }
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", video.getId());
        result.put("title", video.getTitle());
        result.put("coverUrl", video.getCoverUrl());
        return Result.success(result);
    }

    // 删除
    @DeleteMapping("/admin/delete/{id}")
    public Result delete(@PathVariable Long id) {
        // 先获取视频信息
        Video video = videoMapper.selectById(id);
        if (video != null) {
            // 删除本地视频文件
            if (video.getVideoUrl() != null) {
                String videoFilename = video.getVideoUrl().replace(VIDEO_URL_PREFIX, "");
                File videoFile = new File(VIDEO_PATH + videoFilename);
                if (videoFile.exists()) {
                    videoFile.delete();
                }
            }
            // 删除本地封面文件
            if (video.getCoverUrl() != null) {
                String coverFilename = video.getCoverUrl().replace(COVER_URL_PREFIX, "");
                File coverFile = new File(COVER_PATH + coverFilename);
                if (coverFile.exists()) {
                    coverFile.delete();
                }
            }
        }
        // 从数据库中删除记录
        videoMapper.deleteById(id);
        return Result.success();
    }

    // 数据统计
    @GetMapping("/admin/stat")
    public Result stat() {
        List<Video> all = videoMapper.selectList(null);
        List<Video> valid = all;

        Map<String, Object> stat = new HashMap<>();
        stat.put("totalCount", valid.size());
        stat.put("todayCount", 0);
        stat.put("normalCount", (int) valid.stream().filter(v -> v.getStatus() == 0).count());
        stat.put("offlineCount", (int) valid.stream().filter(v -> v.getStatus() == 1).count());

        Map<String, Long> userMap = valid.stream().collect(Collectors.groupingBy(Video::getUsername, Collectors.counting()));
        List<Map<String, Object>> rank = new ArrayList<>();
        for (Map.Entry<String, Long> e : userMap.entrySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("username", e.getKey());
            m.put("videoCount", e.getValue());
            rank.add(m);
        }
        rank.sort((a, b) -> Long.compare((Long) b.get("videoCount"), (Long) a.get("videoCount")));

        Map<String, Object> res = new HashMap<>();
        res.put("stat", stat);
        res.put("userRank", rank);
        return Result.success(res);
    }

    // 清空所有视频数据
    @DeleteMapping("/admin/clearAll")
    public Result clearAll() {
        // 获取所有视频
        List<Video> allVideos = videoMapper.selectList(null);
        
        // 删除本地文件
        for (Video video : allVideos) {
            // 删除本地视频文件
            if (video.getVideoUrl() != null) {
                String videoFilename = video.getVideoUrl().replace(VIDEO_URL_PREFIX, "");
                File videoFile = new File(VIDEO_PATH + videoFilename);
                if (videoFile.exists()) {
                    videoFile.delete();
                }
            }
            // 删除本地封面文件
            if (video.getCoverUrl() != null) {
                String coverFilename = video.getCoverUrl().replace(COVER_URL_PREFIX, "");
                File coverFile = new File(COVER_PATH + coverFilename);
                if (coverFile.exists()) {
                    coverFile.delete();
                }
            }
        }
        
        // 清空数据库
        videoMapper.delete(null);
        
        return Result.success("视频数据已清空");
    }

    // 点赞/取消点赞
    @PostMapping("/like")
    public Result toggleLike(@RequestBody Map<String, Long> params) {
        Long videoId = params.get("videoId");
        Long userId = params.get("userId");
        
        if (videoId == null || userId == null) {
            return Result.error("参数错误");
        }

        LambdaQueryWrapper<VideoLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoLike::getVideoId, videoId).eq(VideoLike::getUserId, userId);
        VideoLike existingLike = videoLikeMapper.selectOne(wrapper);
        
        Video video = videoMapper.selectById(videoId);
        
        if (existingLike != null) {
            videoLikeMapper.deleteById(existingLike.getId());
            video.setLikes(Math.max(0, video.getLikes() - 1));
            videoMapper.updateById(video);
            return Result.success("已取消点赞");
        } else {
            VideoLike like = new VideoLike();
            like.setVideoId(videoId);
            like.setUserId(userId);
            like.setCreateTime(new Date());
            videoLikeMapper.insert(like);
            video.setLikes(video.getLikes() + 1);
            videoMapper.updateById(video);
            
            // 发送通知
            if (video.getUserId() != null && !video.getUserId().equals(userId)) {
                User liker = userMapper.selectById(userId);
                sendNotification(video.getUserId(), "like", liker.getUsername() + "点赞了你的视频", videoId);
            }
            
            return Result.success("点赞成功");
        }
    }

    // 收藏/取消收藏
    @PostMapping("/favorite")
    public Result toggleFavorite(@RequestBody Map<String, Long> params) {
        Long videoId = params.get("videoId");
        Long userId = params.get("userId");
        
        if (videoId == null || userId == null) {
            return Result.error("参数错误");
        }

        LambdaQueryWrapper<VideoFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoFavorite::getVideoId, videoId).eq(VideoFavorite::getUserId, userId);
        VideoFavorite existingFavorite = videoFavoriteMapper.selectOne(wrapper);
        
        Video video = videoMapper.selectById(videoId);
        
        if (existingFavorite != null) {
            videoFavoriteMapper.deleteById(existingFavorite.getId());
            video.setFavorites(Math.max(0, video.getFavorites() - 1));
            videoMapper.updateById(video);
            return Result.success("已取消收藏");
        } else {
            VideoFavorite favorite = new VideoFavorite();
            favorite.setVideoId(videoId);
            favorite.setUserId(userId);
            favorite.setCreateTime(new Date());
            videoFavoriteMapper.insert(favorite);
            video.setFavorites(video.getFavorites() + 1);
            videoMapper.updateById(video);
            
            // 发送通知
            if (video.getUserId() != null && !video.getUserId().equals(userId)) {
                User favoritor = userMapper.selectById(userId);
                sendNotification(video.getUserId(), "favorite", favoritor.getUsername() + "收藏了你的视频", videoId);
            }
            
            return Result.success("收藏成功");
        }
    }

    // 检查当前用户是否点赞/收藏
    @GetMapping("/checkStatus/{videoId}/{userId}")
    public Result checkVideoStatus(@PathVariable Long videoId, @PathVariable Long userId) {
        LambdaQueryWrapper<VideoLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(VideoLike::getVideoId, videoId).eq(VideoLike::getUserId, userId);
        boolean liked = videoLikeMapper.selectCount(likeWrapper) > 0;

        LambdaQueryWrapper<VideoFavorite> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.eq(VideoFavorite::getVideoId, videoId).eq(VideoFavorite::getUserId, userId);
        boolean favorited = videoFavoriteMapper.selectCount(favWrapper) > 0;

        Map<String, Boolean> result = new HashMap<>();
        result.put("liked", liked);
        result.put("favorited", favorited);
        return Result.success(result);
    }
    
    // 获取用户统计数据
    @GetMapping("/userStats/{userId}")
    public Result getUserStats(@PathVariable Long userId) {
        // 作品数
        LambdaQueryWrapper<Video> videoWrapper = new LambdaQueryWrapper<>();
        videoWrapper.eq(Video::getUserId, userId);
        long videosCount = videoMapper.selectCount(videoWrapper);
        
        // 获赞数（该用户所有视频的点赞数之和）
        List<Video> videos = videoMapper.selectList(videoWrapper);
        int totalLikes = videos.stream().mapToInt(v -> v.getLikes() != null ? v.getLikes() : 0).sum();
        
        // 收藏数（该用户收藏的视频数）
        LambdaQueryWrapper<VideoFavorite> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.eq(VideoFavorite::getUserId, userId);
        long favoritesCount = videoFavoriteMapper.selectCount(favWrapper);
        
        // 粉丝数（该用户的粉丝数量）
        LambdaQueryWrapper<UserFollow> followWrapper = new LambdaQueryWrapper<>();
        followWrapper.eq(UserFollow::getFollowingId, userId);
        long followersCount = userFollowMapper.selectCount(followWrapper);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("videosCount", videosCount);
        stats.put("likesCount", totalLikes);
        stats.put("favoritesCount", favoritesCount);
        stats.put("followersCount", followersCount);
        
        return Result.success(stats);
    }
    
    // 关注/取消关注用户
    @PostMapping("/follow")
    public Result toggleFollow(@RequestBody Map<String, Long> params) {
        Long followingId = params.get("followingId");
        Long followerId = params.get("followerId");
        
        if (followingId == null || followerId == null) {
            return Result.error("参数错误");
        }
        
        if (followingId.equals(followerId)) {
            return Result.error("不能关注自己");
        }
        
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowerId, followerId)
              .eq(UserFollow::getFollowingId, followingId);
        UserFollow existing = userFollowMapper.selectOne(wrapper);
        
        if (existing != null) {
            userFollowMapper.deleteById(existing.getId());
            return Result.success("已取消关注");
        } else {
            UserFollow follow = new UserFollow();
            follow.setFollowerId(followerId);
            follow.setFollowingId(followingId);
            follow.setCreateTime(new Date());
            userFollowMapper.insert(follow);
            
            // 发送通知
            User follower = userMapper.selectById(followerId);
            sendNotification(followingId, "follow", follower.getUsername() + "关注了你", followerId);
            
            return Result.success("关注成功");
        }
    }
    
    @GetMapping("/transcodeProgress/{videoId}")
    public Result getTranscodeProgress(@PathVariable Long videoId) {
        TranscodeProgress progress = TranscodeProgress.get(videoId);
        if (progress == null) {
            return Result.success(null);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("videoId", progress.getVideoId());
        result.put("progress", progress.getOverallProgress());
        result.put("status", progress.getStatus());
        result.put("progress480p", progress.getProgress480p());
        result.put("progress720p", progress.getProgress720p());
        result.put("progress1080p", progress.getProgress1080p());
        result.put("task480p", progress.getTask480p());
        result.put("task720p", progress.getTask720p());
        result.put("task1080p", progress.getTask1080p());
        return Result.success(result);
    }
}