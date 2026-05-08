package com.video.platform.user.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.video.platform.user.entity.User;
import com.video.platform.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class AccountCleanupTask {

    @Autowired
    private UserMapper userMapper;

    @Scheduled(cron = "0 0 3 * * ?")
    public void processExpiredAccounts() {
        Date sevenDaysAgo = new Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000);
        
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getDelFlag, 1)
               .eq(User::getDeleteType, 0)
               .le(User::getDeleteTime, sevenDaysAgo);
        
        List<User> expiredUsers = userMapper.selectList(wrapper);
        
        for (User user : expiredUsers) {
            User update = new User();
            update.setId(user.getId());
            update.setDelFlag(2);
            update.setUsername("用户" + user.getId());
            update.setPassword("DELETED");
            update.setQuestion(null);
            update.setAnswer(null);
            update.setAvatar(null);
            update.setDescription(null);
            userMapper.updateById(update);
        }
    }
}
