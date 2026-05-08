DROP DATABASE IF EXISTS video_platform;
CREATE DATABASE IF NOT EXISTS video_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE video_platform;

-- ====================== 1. 用户表 ======================
DROP TABLE IF EXISTS tb_user;
CREATE TABLE tb_user (
                         id BIGINT AUTO_INCREMENT COMMENT '用户ID',
                         username VARCHAR(50) NOT NULL COMMENT '用户名',
                         password VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
                         question VARCHAR(255) DEFAULT NULL COMMENT '密保问题',
                         answer VARCHAR(255) DEFAULT NULL COMMENT '密保答案',
                         phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
                         create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         role VARCHAR(20) DEFAULT 'user' COMMENT 'admin=管理员,user=普通用户',
                         status TINYINT DEFAULT 0 COMMENT '0正常1封禁',
                         del_flag TINYINT DEFAULT 0 COMMENT '0正常1冷静期2永久注销',
                         delete_time DATETIME DEFAULT NULL COMMENT '注销时间',
                         cool_down_hours INT DEFAULT 168 COMMENT '冷静期小时',
                         delete_type TINYINT DEFAULT 0 COMMENT '0自主注销1管理员删除',
                         avatar TEXT COMMENT '用户头像',
                         description VARCHAR(255) DEFAULT NULL COMMENT '用户简介',
                         PRIMARY KEY (id),
                         UNIQUE KEY uk_username (username)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ====================== 2. 管理员注册码表 ======================
DROP TABLE IF EXISTS tb_admin_code;
CREATE TABLE tb_admin_code (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               code VARCHAR(50) NOT NULL UNIQUE COMMENT '管理员注册码',
                               used TINYINT DEFAULT 0 COMMENT '0未使用 1已使用',
                               create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='管理员注册码表';

-- ====================== 3. 初始化注册码 ======================
INSERT INTO tb_admin_code (code) VALUES ('ADMIN2025'), ('ADMIN8888');

-- ====================== 4. 视频表 ======================
DROP TABLE IF EXISTS tb_video;
CREATE TABLE tb_video (
                          id BIGINT AUTO_INCREMENT COMMENT '视频ID',
                          title VARCHAR(100) NOT NULL COMMENT '视频标题',
                          intro VARCHAR(500) DEFAULT '' COMMENT '视频简介',
                          video_url VARCHAR(255) NOT NULL COMMENT '视频存储地址',
                          cover_url VARCHAR(255) DEFAULT '' COMMENT '视频封面地址',
                          duration INT DEFAULT 0 COMMENT '视频时长(秒)',
                          user_id BIGINT NOT NULL COMMENT '上传用户ID',
                          username VARCHAR(50) NOT NULL COMMENT '上传用户名',
                          create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
                          status TINYINT DEFAULT 0 COMMENT '0正常 1下架',
                          video_type VARCHAR(50) DEFAULT '' COMMENT '视频类型',
                          likes INT DEFAULT 0 COMMENT '点赞数',
                          favorites INT DEFAULT 0 COMMENT '收藏数',
                          views INT DEFAULT 0 COMMENT '播放数',
                          review_status TINYINT DEFAULT 0 COMMENT '审核状态: 0待审核 1通过 2拒绝',
                          review_time DATETIME DEFAULT NULL COMMENT '审核时间',
                          reviewer_id BIGINT DEFAULT NULL COMMENT '审核人ID',
                          reviewer_name VARCHAR(50) DEFAULT NULL COMMENT '审核人用户名',
                          reject_reason VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因',
                          pending_title VARCHAR(100) DEFAULT NULL COMMENT '待审核的新标题',
                          pending_intro VARCHAR(500) DEFAULT NULL COMMENT '待审核的新简介',
                          pending_cover_url VARCHAR(255) DEFAULT NULL COMMENT '待审核的新封面地址',
                          pending_video_type VARCHAR(50) DEFAULT NULL COMMENT '待审核的新分类',
                          pending_video_url VARCHAR(255) DEFAULT NULL COMMENT '待审核的新视频地址',
                          has_pending_edit TINYINT DEFAULT 0 COMMENT '是否有待审核的修改: 0否 1是',
                          -- 多码率视频URL
                          video_url_480p VARCHAR(255) DEFAULT NULL COMMENT '480P视频URL',
                          video_url_720p VARCHAR(255) DEFAULT NULL COMMENT '720P视频URL',
                          video_url_1080p VARCHAR(255) DEFAULT NULL COMMENT '1080P视频URL',
                          PRIMARY KEY (id),
                          KEY idx_user_id (user_id)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='视频信息表';

-- ====================== 4.1 视频点赞表 ======================
DROP TABLE IF EXISTS tb_video_like;
CREATE TABLE tb_video_like (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  video_id BIGINT NOT NULL COMMENT '视频ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  UNIQUE KEY uk_video_user (video_id, user_id)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='视频点赞表';

-- ====================== 4.2 视频收藏表 ======================
DROP TABLE IF EXISTS tb_video_favorite;
CREATE TABLE tb_video_favorite (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  video_id BIGINT NOT NULL COMMENT '视频ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  UNIQUE KEY uk_video_user (video_id, user_id)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='视频收藏表';

-- ====================== 4.3 用户关注表 ======================
DROP TABLE IF EXISTS tb_user_follow;
CREATE TABLE tb_user_follow (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  follower_id BIGINT NOT NULL COMMENT '粉丝ID',
  following_id BIGINT NOT NULL COMMENT '被关注用户ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  UNIQUE KEY uk_follower_following (follower_id, following_id)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注表';

-- ====================== 4.4 消息通知表 ======================
DROP TABLE IF EXISTS tb_notification;
CREATE TABLE tb_notification (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '接收用户ID',
  type VARCHAR(20) NOT NULL COMMENT '通知类型: like点赞 favorite收藏 follow关注 video_upload视频上传 video_review审核 comment评论 comment_reply回复评论',
  content VARCHAR(500) NOT NULL COMMENT '通知内容',
  related_id BIGINT DEFAULT NULL COMMENT '相关ID(视频ID/用户ID/评论ID等)',
  video_id BIGINT DEFAULT NULL COMMENT '相关视频ID(评论通知时使用)',
  is_read TINYINT DEFAULT 0 COMMENT '是否已读: 0未读 1已读',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_user_id (user_id),
  KEY idx_create_time (create_time)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';

-- ====================== 5. 视频评论表 ======================
DROP TABLE IF EXISTS tb_comment;
CREATE TABLE tb_comment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  video_id BIGINT NOT NULL COMMENT '视频ID',
  user_id BIGINT NOT NULL COMMENT '评论用户ID',
  content VARCHAR(500) NOT NULL COMMENT '评论内容',
  parent_id BIGINT DEFAULT NULL COMMENT '父评论ID（用于回复）',
  reply_to_user_id BIGINT DEFAULT NULL COMMENT '回复目标用户ID',
  like_count INT DEFAULT 0 COMMENT '点赞数',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_video_id (video_id),
  KEY idx_user_id (user_id),
  KEY idx_create_time (create_time)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='视频评论表';

-- ====================== 5.1 评论点赞表 ======================
DROP TABLE IF EXISTS tb_comment_like;
CREATE TABLE tb_comment_like (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  comment_id BIGINT NOT NULL COMMENT '评论ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_comment_user (comment_id, user_id)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞表';

-- ====================== 6. 分类表 ======================
DROP TABLE IF EXISTS tb_category;
CREATE TABLE tb_category (
                             id BIGINT AUTO_INCREMENT COMMENT '分类ID',
                             name VARCHAR(50) NOT NULL COMMENT '分类名称',
                             value VARCHAR(50) NOT NULL COMMENT '分类值',
                             sort INT DEFAULT 0 COMMENT '排序',
                             create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             PRIMARY KEY (id),
                             UNIQUE KEY uk_value (value)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='视频分类表';

-- ====================== 6. 初始化分类数据 ======================
INSERT INTO tb_category (name, value, sort) VALUES
('短视频', 'short', 1),
('长视频', 'long', 2),
('直播', 'live', 3),
('纪录片', 'documentary', 4),
('动画', 'animation', 5),
('其他', 'other', 6);