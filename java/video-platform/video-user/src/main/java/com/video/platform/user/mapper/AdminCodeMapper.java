package com.video.platform.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.video.platform.user.entity.AdminCode;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface AdminCodeMapper extends BaseMapper<AdminCode> {
    @Update("update tb_admin_code set used=1 where code=#{code}")
    void markUsed(@Param("code") String code);
}