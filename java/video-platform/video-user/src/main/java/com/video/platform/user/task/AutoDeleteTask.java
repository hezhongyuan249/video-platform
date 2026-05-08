package com.video.platform.user.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.video.platform.user.entity.User;
import com.video.platform.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Component
public class AutoDeleteTask {

    @Autowired
    private UserMapper userMapper;

    @Scheduled(cron = "0 * * * * ?")
    public void clearExpiredUsers() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDelFlag, 1);
        List<User> list = userMapper.selectList(wrapper);

        for (User user : list) {
            Date deleteTime = user.getDeleteTime();
            int hours = user.getCoolDownHours();

            Calendar cal = Calendar.getInstance();
            cal.setTime(deleteTime);
            cal.add(Calendar.HOUR, hours);
            Date expire = cal.getTime();

            if (new Date().after(expire)) {
                userMapper.deleteById(user.getId());
                System.out.println("用户ID:" + user.getId() + " 已到期自动永久删除");
            }
        }
    }
}
