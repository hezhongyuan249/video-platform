package com.video.platform.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

@TableName("tb_admin_code")
public class AdminCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private Integer used;
    private Date createTime;

    public AdminCode() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getUsed() { return used; }
    public void setUsed(Integer used) { this.used = used; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}