package com.example.italker.service;

import com.example.italker.mapper.UserMapper;
import com.example.italker.pojo.entity.User;
//import com.example.italker.utils.SessionFac;
import com.example.italker.utils.TextUtil;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * @author zhaoyu
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PushService pushService;

    /**
     *
     *@description:通过phone找到User
     *@time: 2020/1/9
     *@methodName:
     */
    public User findByPhone(String phone){
        return userMapper.findByPhone(phone);
    }


    /**
     * 更新用户信息到数据库
     *
     * @param user User
     * @return User
     */
    public User update(User user){
        System.out.println("token = "+ user.getToken());
        userMapper.saveOrUpdate(user);
        System.out.println("token = "+ user.getToken());
        user = userMapper.findUserById(user.getId());
        return user;
    }

    /**
     *
     *@description:通过名字找到User
     *@time: 2020/1/9
     *@methodName:
     */
    public User findByName(String name) {
        return userMapper.findByName(name);
    }

    public User register(String account,String password,String name){
        //去除账户中的首位空格
        account = account.trim();
        //处理密码
        password = encodePassword(password);

        User user = createUser(account,password,name);
        if(user != null){
            user = login(user);
        }
        return user;
    }

    /**
     * 对密码进行加密操作
     *
     * @param password 原文
     * @return 密文
     */
    private  String encodePassword(String password) {
        // 密码去除首位空格
        password = password.trim();
        // 进行MD5非对称加密，加盐会更安全，盐也需要存储
        password = TextUtil.getMD5(password);
        // 再进行一次对称的Base64加密，当然可以采取加盐的方案
        return TextUtil.encodeBase64(password);
    }

    /**
     * 把一个User进行登录操作
     * 本质上是对Token进行操作
     *
     * @param user User
     * @return User
     */
    private  User login(User user) {
        // 使用一个随机的UUID值充当Token
        String newToken = UUID.randomUUID().toString();
        // 进行一次Base64格式化
        newToken = TextUtil.encodeBase64(newToken);
        user.setToken(newToken);

        return update(user);
    }

    /**
     * 注册部分的新建用户逻辑
     *
     * @param account  手机号
     * @param password 加密后的密码
     * @param name     用户名
     * @return 返回一个用户
     */
    private  User createUser(String account, String password, String name) {
        User user = new User();
        user.setName(name);
        user.setPassword(password);
        // 账户就是手机号
        user.setPhone(account);

        // 数据库存储
        Integer id = userMapper.insertUser(user);
        user = userMapper.findUserById(user.getId());
        return user;
    }

    public User bindPushId(User user,String pushId){
        if (Strings.isNullOrEmpty(pushId)) {
            return null;
        }
        // 第一步，查询是否有其他账户绑定了这个设备
        // 取消绑定，避免推送混乱
        // 查询的列表不能包括自己
        @SuppressWarnings("unchecked")
        List<User> userList = userMapper.findByPushId(pushId.toLowerCase(),user.getId());
        for (User u : userList) {
            // 更新为null
            u.setPushId(null);
            userMapper.saveOrUpdate(u);
        }

        if(pushId.equalsIgnoreCase(user.getPushId())){
            //如果已经绑定过了就不需要额外绑定
            return user;
        }else {
            // 如果当前账户之前的设备Id，和需要绑定的不同
            // 那么需要单点登录，让之前的设备退出账户，
            // 给之前的设备推送一条退出消息
            if (Strings.isNullOrEmpty(user.getPushId())) {
                //推送一个退出消息
                //pushService.pushLogout(user,user.getPushId());
                // TODO 推送一个退出消息
            }
            //更新新的设备Id
            user.setPushId(pushId);
            return update(user);

        }
    }

    /**
     * 使用账户和密码进行登录
     */
    public User login(String account, String password) {
        final String accountStr = account.trim();
        //把原文进行同样处理，才能匹配
        final String encodePassword = encodePassword(password);
        //查找用户
        User user = userMapper.loginFind(account,encodePassword);

        if (user != null) {
            // 对User进行登录操作，更新Token
            user = login(user);
        }
        return user;
    }
}
