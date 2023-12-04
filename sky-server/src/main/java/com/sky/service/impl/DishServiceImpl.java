package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品和对应口味
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {//多表操作，为了保持一致性，加事务注解
        //向菜品表插入1条数据
        Dish dish=new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.insert(dish);
        //获取insert语句生成的主键值
        Long dishId = dish.getId();
        //向口味表插入n条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors!=null &&flavors.size()>0) {
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dishId);
            }
            //向表中插入N条数据
            dishFlavorMapper.insertBatch(flavors);
        }

    }

    /**
     * 菜品分页查询
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        Dish dish=new Dish();
        //分页查询
        int page=dishPageQueryDTO.getPage();
        int pageSize = dishPageQueryDTO.getPageSize();
        PageHelper.startPage(page,pageSize);
        Page<DishVO> page1=dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page1.getTotal(),page1);
    }

    /**
     * 菜品批量删除
     * @param ids
     */
    @Override
    @Transactional//涉及到多个表的操作，加事务注解保证数据一致性
    public void deleteBatch(List<Long> ids) {
        //判断当前菜品是否能够删除---是否存在起售中的菜品?
        for (Long id : ids) {
                Dish dish= dishMapper.getById(id);
                if(dish.getStatus()== StatusConstant.ENABLE){
                    //当前菜品处于起售中，不能删除
                    throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
                }
        }
        //判断当前菜品是否能够删除---是否被套餐关联了? ?
        List<Long> setmealIds=setmealDishMapper.getSetmealIdByDishId(ids);
        if(setmealIds != null&&setmealIds.size()>0){
                //当前菜品关联了套餐，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //删除菜品表中的菜品数据
      /*  for (Long id : ids) {
            dishMapper.deleteById(id);
            //删除菜品关联的口味数据
            dishFlavorMapper.deleteByDishId(id);
        }*/
        //根据菜品ids集合批量删除菜品数据
        dishMapper.deleteByIds(ids);
        //根据菜品ids集合批量删除关联的口味数据
        dishFlavorMapper.deleteByDishIds(ids);

    }

}
