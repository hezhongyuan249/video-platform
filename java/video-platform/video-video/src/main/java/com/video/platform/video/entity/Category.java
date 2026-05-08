package com.video.platform.video.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

@TableName("tb_category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;        // 分类名称
    private String value;       // 分类值
    private Integer sort;       // 排序字段
    private Date createTime;    // 创建时间

    // 无参构造
    public Category() {
    }

    // ================ Getter & Setter ================
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    // ================ toString ================
    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", sort=" + sort +
                ", createTime=" + createTime +
                '}';
    }
}