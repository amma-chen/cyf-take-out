package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
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

    /**
     * 批量保存套餐菜品
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据套餐ids删除套餐对应的菜品
     * @param ids
     */
    void deleteBySetmealIds(List<Long> ids);

    /**
     * 根据setmealId查询套餐对应的菜品
     * @param id
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id=#{id}")
    List<SetmealDish> getBySetmealId(Long id);

    /**
     * 修改套餐对应的菜品
     * @param setmealDish
     */
    void update(SetmealDish setmealDish);

    /**
     * 根据套餐id删除套餐对应的菜品
     * @param setmealId
     */
    @Delete("delete from setmeal_dish where setmeal_id=#{setmealId}")
    void deleteBySetmealId(Long setmealId);
}
