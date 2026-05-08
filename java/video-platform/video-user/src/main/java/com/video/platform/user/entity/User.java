package com.video.platform.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

@TableName("tb_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String question;
    private String answer;
    private String phone;
    private Date createTime;
    private String role;
    private Integer status;
    private Integer delFlag;
    private Date deleteTime;
    private Integer coolDownHours;
    private Integer deleteType;
    private String avatar;
    private String description;

    // 临时字段，不入库
    private transient Integer followersCount = 0;
    private transient Integer likesCount = 0;
    private transient Integer favoritesCount = 0;
    private transient Integer videosCount = 0;

    // 你原有的临时字段（全部保留）
    private transient String newPassword;
    private transient String oldPassword;
    // 🔥 仅新增：管理员注册码（临时字段，不入库）
    private transient String adminCode;

    public User() {}

    // ======== 你原有的所有 Getter/Setter（完全不变） ========
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getDelFlag() { return delFlag; }
    public void setDelFlag(Integer delFlag) { this.delFlag = delFlag; }
    public Date getDeleteTime() { return deleteTime; }
    public void setDeleteTime(Date deleteTime) { this.deleteTime = deleteTime; }
    public Integer getCoolDownHours() { return coolDownHours; }
    public void setCoolDownHours(Integer coolDownHours) { this.coolDownHours = coolDownHours; }
    public Integer getDeleteType() { return deleteType; }
    public void setDeleteType(Integer deleteType) { this.deleteType = deleteType; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getFollowersCount() { return followersCount; }
    public void setFollowersCount(Integer followersCount) { this.followersCount = followersCount; }
    public Integer getLikesCount() { return likesCount; }
    public void setLikesCount(Integer likesCount) { this.likesCount = likesCount; }
    public Integer getFavoritesCount() { return favoritesCount; }
    public void setFavoritesCount(Integer favoritesCount) { this.favoritesCount = favoritesCount; }
    public Integer getVideosCount() { return videosCount; }
    public void setVideosCount(Integer videosCount) { this.videosCount = videosCount; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }

    // 🔥 仅新增：adminCode 的 get/set
    public String getAdminCode() { return adminCode; }
    public void setAdminCode(String adminCode) { this.adminCode = adminCode; }

    // 你原有的 toString（不变）
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", question='" + question + '\'' +
                ", answer='" + answer + '\'' +
                ", phone='" + phone + '\'' +
                ", createTime=" + createTime +
                ", role='" + role + '\'' +
                ", status=" + status +
                ", delFlag=" + delFlag +
                ", deleteTime=" + deleteTime +
                ", coolDownHours=" + coolDownHours +
                ", deleteType=" + deleteType +
                '}';
    }
}