package com.sky.mapper;

import com.sky.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

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

    /**
     * 根据用户id查找用户
     * @param userId
     * @return
     */
    @Select("select *from user where id=#{userId}")
    public User getById(Long userId);

    /**
     * 查询注册时间范围内的新用户总数
     * @param beginTime
     * @param endTime
     * @return
     */
    @Select("select count(id) from user where create_time between#{beginTime} and #{endTime}")
    Integer countNewUserByCreateTime(LocalDateTime beginTime, LocalDateTime endTime);

    /**
     * 查询注册时间范围内的所有用户总数
     * @param endTime
     * @return
     */
    @Select("select count(id)from user where create_time <= #{endTime}")
    Integer countTotalUserByCreateTime( LocalDateTime endTime);
}
