package com.video.platform.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.platform.common.result.Result;
import com.video.platform.user.entity.AdminCode;
import com.video.platform.user.entity.User;
import com.video.platform.user.mapper.AdminCodeMapper;
import com.video.platform.user.mapper.UserMapper;
import com.video.platform.user.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AdminCodeMapper adminCodeMapper;
    @Autowired
    private TokenService tokenService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    // 登录：仅自行注销用户自动恢复
    @PostMapping("/login")
    public Result login(@RequestBody User user, HttpSession session) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        User dbUser = userMapper.selectOne(wrapper);

        if (dbUser == null) {
            return Result.error("用户不存在");
        }
        if (!dbUser.getRole().equals(user.getRole())) {
            return Result.error("角色选择错误");
        }
        if (!passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            return Result.error("密码错误");
        }

        // 仅【自行注销(deleteType=0)】的账号 登录自动恢复
        if (dbUser.getDelFlag() == 1 && dbUser.getDeleteType() == 0) {
            User recoverUser = new User();
            recoverUser.setId(dbUser.getId());
            recoverUser.setDelFlag(0);
            recoverUser.setDeleteTime(null);
            recoverUser.setDeleteType(null);
            recoverUser.setCoolDownHours(null);
            userMapper.updateById(recoverUser);
            
            // 生成 token
            Map<String, Object> tokenData = tokenService.generateToken(dbUser.getId());
            
            Map<String, Object> data = new HashMap<>();
            data.put("user", dbUser);
            data.put("recover", true);
            data.put("token", tokenData.get("token"));
            return Result.success(data);
        }

        // 生成 token
        Map<String, Object> tokenData = tokenService.generateToken(dbUser.getId());
        
        Map<String, Object> result = new HashMap<>();
        result.put("user", dbUser);
        result.put("token", tokenData.get("token"));
        return Result.success(result);
    }

    // 获取当前登录用户
    @GetMapping("/current")
    public Result getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.error("未登录");
        }
        
        String token = authHeader.substring(7);
        Long userId = tokenService.getUserIdFromToken(token);
        
        if (userId == null) {
            return Result.error("token无效");
        }
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        return Result.success(user);
    }

    // 注册
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        User one = userMapper.selectOne(wrapper);

        if (one != null) {
            return Result.error("用户名已存在");
        }

        if ("admin".equals(user.getRole())) {
            if (user.getAdminCode() == null || user.getAdminCode().trim().isEmpty()) {
                return Result.error("请输入管理员注册码");
            }
            LambdaQueryWrapper<AdminCode> codeWrapper = new LambdaQueryWrapper<>();
            codeWrapper.eq(AdminCode::getCode, user.getAdminCode()).eq(AdminCode::getUsed, 0);
            AdminCode code = adminCodeMapper.selectOne(codeWrapper);
            if (code == null) {
                return Result.error("注册码无效或已使用");
            }
            adminCodeMapper.markUsed(user.getAdminCode());
        }

        user.setCreateTime(new Date());
        if (user.getRole() == null) {
            user.setRole("user");
        }
        user.setStatus(0);
        user.setDelFlag(0);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
        return Result.success();
    }

    // 获取密保问题
    @GetMapping("/question")
    public Result getQuestion(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username).select(User::getQuestion);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user.getQuestion());
    }

    // 重置密码
    @PostMapping("/resetPwd")
    public Result resetPwd(@RequestBody User user) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        User one = userMapper.selectOne(wrapper);

        if (one == null) {
            return Result.error("用户不存在");
        }
        if (!one.getAnswer().equals(user.getAnswer())) {
            return Result.error("密保答案错误");
        }

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUsername, user.getUsername())
                .set(User::getPassword, passwordEncoder.encode(user.getNewPassword()));
        userMapper.update(null, updateWrapper);

        return Result.success();
    }

    // 用户获取自己信息
    @GetMapping("/info")
    public Result getUserInfo(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    // ===================== 管理员功能 =====================
    // 管理员用户列表（分页 + 模糊搜索）
    @GetMapping("/admin/list")
    public Result userList(
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDelFlag, 0);
        if (StringUtils.hasText(username)) {
            wrapper.like(User::getUsername, username);
        }

        Page<User> page = new Page<>(pageNum, pageSize);
        userMapper.selectPage(page, wrapper);

        return Result.success(page);
    }

    // 管理员物理硬删除
    @DeleteMapping("/admin/delete/{id}")
    public Result deleteUser(@PathVariable Long id) {
        userMapper.deleteById(id);
        return Result.success();
    }

    // 管理员封禁/解封
    @PostMapping("/admin/changeStatus")
    public Result changeStatus(@RequestBody User user) {
        userMapper.updateById(user);
        return Result.success();
    }

    // ===================== 用户个人功能 =====================
    // 修改用户名
    @PostMapping("/updateUsername")
    public Result updateUsername(@RequestBody User user) {
        User dbUser = userMapper.selectById(user.getId());
        if (!passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            return Result.error("密码错误，无法修改用户名");
        }
        User update = new User();
        update.setId(user.getId());
        update.setUsername(user.getUsername());
        userMapper.updateById(update);
        return Result.success();
    }

    // 修改头像
    @PostMapping("/updateAvatar")
    public Result updateAvatar(@RequestBody User user) {
        if (user.getId() == null || user.getAvatar() == null) {
            return Result.error("参数错误");
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            return Result.error("请输入密码验证");
        }
        User dbUser = userMapper.selectById(user.getId());
        if (!passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            return Result.error("密码错误");
        }
        User update = new User();
        update.setId(user.getId());
        update.setAvatar(user.getAvatar());
        userMapper.updateById(update);
        return Result.success();
    }

    // 获取用户主页信息
    @GetMapping("/profile")
    public Result getProfile(@RequestParam Long userId) {
        if (userId == null) {
            return Result.error("参数错误");
        }
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        // 设置统计数据（暂时设为0，后续可扩展）
        user.setFollowersCount(0);
        user.setLikesCount(0);
        user.setFavoritesCount(0);
        user.setVideosCount(0);
        
        return Result.success(user);
    }

    // 更新用户资料
    @PostMapping("/updateProfile")
    public Result updateProfile(@RequestBody User user) {
        if (user.getId() == null) {
            return Result.error("参数错误");
        }
        
        User update = new User();
        update.setId(user.getId());
        if (user.getDescription() != null) {
            update.setDescription(user.getDescription());
        }
        if (user.getAvatar() != null) {
            update.setAvatar(user.getAvatar());
        }
        
        userMapper.updateById(update);
        return Result.success();
    }

    // 修改密码
    @PostMapping("/updatePwd")
    public Result updatePwd(@RequestBody User user) {
        User dbUser = userMapper.selectById(user.getId());
        if (!passwordEncoder.matches(user.getOldPassword(), dbUser.getPassword())) {
            return Result.error("旧密码错误，无法修改密码");
        }
        User update = new User();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.updateById(update);
        return Result.success();
    }

    // 修改密保
    @PostMapping("/updateSecurity")
    public Result updateSecurity(@RequestBody User user) {
        User dbUser = userMapper.selectById(user.getId());
        if (!passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            return Result.error("密码错误，无法修改密保");
        }
        User update = new User();
        update.setId(user.getId());
        update.setQuestion(user.getQuestion());
        update.setAnswer(user.getAnswer());
        userMapper.updateById(update);
        return Result.success();
    }

    // 用户注销账号
    @PostMapping("/cancel")
    public Result cancelAccount(@RequestBody User user) {
        if (user.getId() == null) {
            return Result.error("参数错误");
        }
        
        User dbUser = userMapper.selectById(user.getId());
        if (dbUser == null) {
            return Result.error("用户不存在");
        }
        if (dbUser.getDelFlag() == 1) {
            return Result.error("账号已在注销流程中");
        }
        
        User update = new User();
        update.setId(user.getId());
        update.setDelFlag(1);
        update.setDeleteTime(new Date());
        update.setDeleteType(0);
        userMapper.updateById(update);
        
        return Result.success();
    }

    // 用户自行恢复
    @PostMapping("/recoverSelf")
    public Result recoverSelf(@RequestBody User user) {
        User dbUser = userMapper.selectById(user.getId());
        if (dbUser.getDeleteType() == 1) {
            return Result.error("您的账号由管理员管控，无法自行恢复，请联系管理员！");
        }
        User update = new User();
        update.setId(user.getId());
        update.setDelFlag(0);
        update.setDeleteTime(null);
        update.setDeleteType(null);
        userMapper.updateById(update);
        return Result.success();
    }
}