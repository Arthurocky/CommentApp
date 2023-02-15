package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import static com.hmdp.utils.RedisConstants.Login_User;
import static com.hmdp.utils.RedisConstants.Verification_Code;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private IUserService userService;

    /**
     * 验证码功能
     *
     * @param phone
     * @param session
     * @return
     */
    @Override
    public Result sendCode(String phone, HttpSession session)
    {
        // 1.校验手机号
        boolean phoneInvalid = RegexUtils.isPhoneInvalid(phone);
        // 2.如果不符合，返回错误信息
        if (phoneInvalid) {
            return Result.fail("手机格式错误");
        }
        // 3.符合，生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 4.保存验证码到session
        session.setAttribute(Verification_Code, code);
        // 5.发送验证码
        log.info("发送短信验证码成功，验证码：{}", code);
        System.out.println("验证码: " + code);
        return Result.ok("发送成功");
    }

    /**
     * 登录功能
     *
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session)
    {
        //每个功能的实现最好需要再独立校验一次，防止输入的内容不一致

        String phone = loginForm.getPhone();
        String code = loginForm.getCode();
        //1.校验手机号：
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }

        //2.校验验证码
        String cacheCode = session.getAttribute(Verification_Code).toString();
        if (cacheCode == null || !code.equals(cacheCode)) {
            return Result.fail("验证码错误");
        }

        //3.1若一致，在数据库中根据手机号查询到该用户后继续进行
        //方式一
        User user = query().eq("phone", phone).one();

        //方式二
        //LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        //wrapper.eq(User::getPhone,phone);
        //User user = userService.getById(wrapper);

        //方式三
        //QueryChainWrapper<User> queryChainWrapper = query().eq("User::getPhone", phone);
        //User user = this.getOne(queryChainWrapper);
        //3.1.1若数据库中不存在则创建新用户
        if (user == null) {
            user = creatUserWithPhone(phone);
        }
        //3.1.2若数据库中存在则选择该用户
        session.setAttribute(Login_User, user);
        return Result.ok(user);
    }

    private User creatUserWithPhone(String phone)
    {
        User user = new User();
        user.setPhone(phone);
        //昵称：user_+手机号()或user_+随机十位或user_+uuid
        //user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        //user.setNickName(String.valueOf(UUID.randomUUID()));
        user.setNickName(USER_NICK_NAME_PREFIX + phone);
        this.save(user);
        return user;

    }
}
