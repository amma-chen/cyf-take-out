package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    @Autowired
    private SetmealMapper setmealMapper;

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

    /**
     * 根据id查询菜品及其口味
     * @param id
     * @return
     */
    @Override
    @Transactional
    public DishVO getByIdWithFlavor(Long id) {
        //根据id查询菜品信息
        Dish dish=dishMapper.getById(id);
        //根据dishId查询口味相关信息
        List<DishFlavor> dishFlavor=dishFlavorMapper.getByDishId(id);//一个菜品可能对应多个口味
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        //BeanUtils.copyProperties(dishFlavor,dishVO);不能这么写，dishVo里面没有这四个字段
        dishVO.setFlavors(dishFlavor);
        return dishVO;
    }

    /**
     * 根据id修改菜品基本信息和对应口味信息
     * @param dishDTO
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        //修改菜品表
        Dish dish=new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.update(dish);
        //修改口味表,先删除口味数据再插入口味数据
        //根据dishId删除口味
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        //插入口味数据
        List<DishFlavor> flavors=dishDTO.getFlavors();
        if(flavors!=null &&flavors.size()>0) {
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dishDTO.getId());
            }
            //向表中插入N条数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> list(Long categoryId) {
        Dish dish=new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//只能查询出来启用的
        List<Dish> dishList = dishMapper.list(dish);
        return dishList;
    }

    /**
     * 菜品起售停售
     * @param status
     * @param id
     */
    @Override
    @Transactional
    public void startOrStop(Integer status, Long id) {
        Dish dish= Dish.builder()
                .id(id)
                .status(status)
                .build();
        //更改起售停售
        dishMapper.update(dish);
        //如果操作为停售，则包含此的套餐也要停售
        if(status==StatusConstant.DISABLE){
            List<Long> dishIds= new ArrayList<>();
            dishIds.add(id);

            List<Long> setmealIds = setmealDishMapper.getSetmealIdByDishId(dishIds);
            //循环遍历列表，获取对应启停售状态
            if (setmealIds != null && setmealIds.size() > 0) {
            for (Long setmealId : setmealIds) {
                Setmeal setmeal = setmealMapper.getById(setmealId);
                Integer status1 = setmeal.getStatus();
                //如果套餐也起售，则停售
                if(status1==StatusConstant.ENABLE){
                    setmeal.setStatus(StatusConstant.DISABLE);
                    setmealMapper.update(setmeal);
                }
            }
            }
        }

    }

}
