package com.video.platform.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.video.platform.common.result.Result;
import com.video.platform.user.entity.User;
import com.video.platform.user.mapper.UserMapper;
import com.video.platform.user.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 用户注册
     */
    @Override
    public Result register(User user) {
        // 1. 判断用户名是否已存在（修改：去掉Lambda，永不爆红）
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", user.getUsername());
        User existUser = this.getOne(wrapper);
        if (existUser != null) {
            return Result.error("用户名已存在");
        }
        // 2. 保存用户（密码明文，毕设简化版）
        this.save(user);
        return Result.success("注册成功");
    }

    /**
     * 用户登录
     */
    @Override
    public Result login(User user) {
        // 1. 根据用户名查询用户（修改：去掉Lambda）
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", user.getUsername());
        User loginUser = this.getOne(wrapper);

        // 2. 校验用户和密码
        if (loginUser == null) {
            return Result.error("用户名不存在");
        }
        if (!loginUser.getPassword().equals(user.getPassword())) {
            return Result.error("密码错误");
        }

        // 3. 登录成功，返回用户信息（隐藏密码）
        loginUser.setPassword(null);
        return Result.success(loginUser);
    }
}
