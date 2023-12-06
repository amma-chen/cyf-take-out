package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * 店铺操作接口
 */
@Slf4j
@RestController("userShopController")
@RequestMapping("/user/shop")
@Api("店铺操作接口")
public class ShopController {

    public static final String KEY ="SHOP_STATUS";

    @Autowired
    RedisTemplate redisTemplate;


    /**
     * 查询营业状态
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("查询营业状态")
    public Result<Integer> getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("查询营业状态为：{}",status==1?"营业中":"打烊中");
        return Result.success(status);
    }


}
