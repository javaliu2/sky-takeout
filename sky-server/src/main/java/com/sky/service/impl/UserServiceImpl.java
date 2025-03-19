package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    //微信服务接口地址
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    WeChatProperties weChatProperties;

    @Autowired
    UserMapper userMapper;
    @Override
    public User userLogin(UserLoginDTO userLoginDTO) {
        // 1、调用微信服务获取openid
        String openid = getOpenid(userLoginDTO.getCode());
        // 2、判断是否是新用户
        User user = userMapper.getByOpenid(openid);
        // 2.1 新用户，需要向数据库中插入用户数据
        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
        // 3、返回用户对象
        return user;
    }

    /**
     *
     * @param code 小程序提供的授权码
     * @return openid
     */
    private String getOpenid(String code) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("appid", weChatProperties.getAppid());
        parameters.put("secret", weChatProperties.getSecret());
        parameters.put("js_code", code);
        parameters.put("grant_type", "authorization_code");
        String jsonStr = HttpClientUtil.doGet(WX_LOGIN, parameters);

        JSONObject jsonObject = JSON.parseObject(jsonStr);
        return jsonObject.getString("openid");
    }
}
