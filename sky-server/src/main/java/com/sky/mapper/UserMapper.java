package com.sky.mapper;

import com.sky.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    /**
     * 查询openid是否存在
     * @param openid
     * @return
     */
    @Select("select * from user where openid=#{openid}")
    public User getByOpenid(String openid);

    /**
     * 注册对象,并返回id
     * @param user
     * @return
     */
    public void insert(User user);
}
