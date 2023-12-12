package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    UserMapper userMapper;

    //为相信服务接口地址
    public static final String WX_LOGIN="https://api.weixin.qq.com/sns/jscode2session?";
    public static final String GRANT_TYPE="authorization_code";

    /**
     * 微信用户登录功能
     * @param userLoginDTO
     * @return
     */
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        //调用微信接口服务，获取openid的方法
        String openid = getOpenid(userLoginDTO.getCode());
        //判断openid是否为空，若为空登陆失败
        if(openid==null){;
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //判断不为空，是合法微信用户
        //判断是否为新用户，查用户表，如果是新用户自定注册(分装user对象返回)
        User user = userMapper.getByOpenid(openid);
        if (user==null)
        {
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
        log.info("user,{}",user);
        //返回对象
        return user;
    }

    /**
     * 调用微信接口服务，获取微信用户openid
     * @param code
     * @return
     */
    private String getOpenid(String code){
        //调用微信接口服务，获取openid
        Map<String, String> map=new HashMap<>();
        map.put("appid",weChatProperties.getAppid());
        map.put("secret",weChatProperties.getSecret());
        map.put("js_code",code);
        map.put("grant_type",GRANT_TYPE);
        String json = HttpClientUtil.doGet(WX_LOGIN, map);
        //接收返回值并解析提取openid
        JSONObject jsonObject = JSON.parseObject(json);
        String openid = jsonObject.getString("openid");
        return openid;
    }
}
