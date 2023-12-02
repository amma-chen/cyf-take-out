package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

/*    @Autowired
    private HttpServletRequest request;

    @Autowired
    private JwtProperties jwtProperties;*/
    //提取token的复杂方法

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
         String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // 对前端出过来的明文密码进行md5加密处理，然后再进行比对
        password = DigestUtils.md5DigestAsHex(password.getBytes());//getbytes转成bytes数组

        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * 新增员工
     * @param employeeDTO
     */
    @Override
    public void save(EmployeeDTO employeeDTO) {//传进来的dto是为了方便封装前端提交的数据，给持久层建议使用实体类将dto转成实体
        Employee employee=new Employee();
        //对象属性拷贝
        BeanUtils.copyProperties(employeeDTO,employee);//源对象拷贝给目标对象，前提是属性名一致
        employee.setStatus(StatusConstant.ENABLE);//调用了设置了的状态常量
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));//调用了设置的密码常量
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
/*        String jwt = request.getHeader(jwtProperties.getAdminTokenName());
        Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(),jwt);
        log.info("对象{}",claims.get("empId"));
        Long operateUser = Long.valueOf(String.valueOf(claims.get("empId")));*/
        //后期需要改为当前登录用户id
        Long operateUser = BaseContext.getCurrentId();
        employee.setCreateUser(operateUser);
        employee.setUpdateUser(operateUser);
        BaseContext.removeCurrentId();
        log.info("目标对象操作人id:{}",operateUser);
        employeeMapper.insert(employee);
    }

    /**
     *员工分页查询
     * @param employeePageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        String name = employeePageQueryDTO.getName();
        int page = employeePageQueryDTO.getPage();
        int pageSize = employeePageQueryDTO.getPageSize();
        PageHelper.startPage(page,pageSize);
/*        List<Employee> empList=employeeMapper.pageQuery(employeePageQueryDTO);
        Page<Employee> page1=(Page<Employee>) empList;*/
        Page<Employee> page1=employeeMapper.pageQuery(employeePageQueryDTO);
        PageResult pageResult = new PageResult(page1.size(), page1);//page1.getTotal()
        return pageResult;
    }

}
