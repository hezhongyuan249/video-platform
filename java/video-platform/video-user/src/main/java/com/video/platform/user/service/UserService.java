package com.video.platform.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.video.platform.common.result.Result;
import com.video.platform.user.entity.User;

public interface UserService extends IService<User> {
    // 用户注册
    Result register(User user);
    // 用户登录
    Result login(User user);
}
