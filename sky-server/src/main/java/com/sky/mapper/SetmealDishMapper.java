package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根据菜品id查询对应的套餐id
     * @param dishIds
     * @return
     */
    //@Select("select setmeal_id from setmeal_dish where dish_id in #{dishIds}")
    List<Long> getSetmealIdByDishId(List<Long> dishIds);

}
