package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 套餐业务实现
 */
@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        //传入套餐信息
        Setmeal setmeal=new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmeal.setStatus(StatusConstant.DISABLE);
        setmealMapper.insert(setmeal);
        //获取insert语句生成的套餐主键值
        Long setmealId = setmeal.getId();
        //传入菜品信息
        List<SetmealDish> setmealDishes=setmealDTO.getSetmealDishes();
        //设置套餐的id
        if (setmealDishes!=null && setmealDishes.size()>0) {
            for (SetmealDish setmealDish : setmealDishes) {
                setmealDish.setSetmealId(setmealId);
            }
        }
        //批量保存套餐菜品
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        //设置分页查询参数
        int page=setmealPageQueryDTO.getPage();
        int pageSize = setmealPageQueryDTO.getPageSize();
        PageHelper.startPage(page,pageSize);

        Page<SetmealVO> page1 =setmealMapper.pageQuery(setmealPageQueryDTO);

        PageResult pageResult=new PageResult(page1.getTotal(),page1);
        return pageResult;
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断套餐是否为起售状态,如果是则抛出异常
        for (Long id : ids) {
            Setmeal setmeal=setmealMapper.getById(id);
            if (setmeal.getStatus()==StatusConstant.ENABLE)
            {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        //不是起售状态则删除套餐
        setmealMapper.deleteByIds(ids);
        //删除套餐对应的菜品
        setmealDishMapper.deleteBySetmealIds(ids);
    }

    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    @Override
    public SetmealVO getById(Long id) {
        SetmealVO setmealVO=new SetmealVO();
        //查询套餐
        Setmeal setmeal=setmealMapper.getById(id);
        BeanUtils.copyProperties(setmeal,setmealVO);
        //根据setmealId查询套餐对应的菜品
        List<SetmealDish> setmealDishes=setmealDishMapper.getBySetmealId(id);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        //修改setmeal表
        Setmeal setmeal=new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.update(setmeal);
        //获取setmealId
        Long setmealId = setmealDTO.getId();
        //删除setmealId对应的setmealdish表所有的菜品
        setmealDishMapper.deleteBySetmealId(setmealId);
        //获取更新的菜品集合
        List<SetmealDish> setmealDishes=setmealDTO.getSetmealDishes();
        //由于套餐菜品表的套餐id是非必须的，因此要手动遍历添加setmealId
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmealId);
        }
        //添加对应的setmealdish表所有的菜品
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 起售禁售套餐
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status,Long id) {
        //判断status是几,是1则是需要更改为起售,需要判断菜品是否都为起售
        if (status==StatusConstant.ENABLE)
        {
            //根据菜品id调用dish表查看菜品状态,其中要求菜品id在setmealdish表对应的套餐id等于传进去的id
            List<Dish> dishes=dishMapper.getBySetmealId(id);
            //如果状态是停售0，则抛出异常不能起售
            for (Dish dish : dishes) {
                if (dish.getStatus()==StatusConstant.DISABLE){
                    throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                }
            }
        }
        Setmeal setmeal=new Setmeal();
        setmeal.setId(id);
        setmeal.setStatus(status);
        setmealMapper.update(setmeal);
    }
}
