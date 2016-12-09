package com.gs.controller;

import com.gs.bean.User;
import com.gs.common.Constants;
import com.gs.common.bean.ControllerResult;
import com.gs.common.bean.Pager;
import com.gs.common.bean.Pager4EasyUI;
import com.gs.common.util.EncryptUtil;
import com.gs.common.util.PagerUtil;
import com.gs.common.web.SessionUtil;
import com.gs.service.UserService;
import org.apache.ibatis.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by WangGenshen on 5/16/16.
 */
@Controller
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Resource
    private UserService userService;


    @ResponseBody
    @RequestMapping(value = "login", method = RequestMethod.POST)
    public ControllerResult login(User user, @Param("checkCode")String checkCode, HttpSession session) {
        if (SessionUtil.isUser(session)) {
            return ControllerResult.getSuccessResult("登录成功");
        }
        String codeInSession = (String) session.getAttribute(Constants.SESSION_CHECK_CODE);
        if(checkCode != null && checkCode.equals(codeInSession)) {
            user.setPwd(EncryptUtil.md5Encrypt(user.getPwd()));
            User c = userService.query(user);
            if (c != null) {

                return ControllerResult.getSuccessResult("登录成功");
            } else {
                return ControllerResult.getFailResult("登录失败,请检查邮箱或密码");
            }
        } else {
            return ControllerResult.getFailResult("验证码错误");
        }
    }

    @RequestMapping(value = "logout", method = RequestMethod.GET)
    public String logout(HttpSession session) {
        session.removeAttribute(Constants.SESSION_CUSTOMER);
        return "redirect:/index";
    }

    @RequestMapping(value = "reg_page", method = RequestMethod.GET)
    public String toRegPage(Model model) {
        model.addAttribute(new User());
        return "user/register";
    }

    @RequestMapping(value = "reg", method = RequestMethod.POST)
    public String reg(User user, HttpSession session) {
        user.setPwd(EncryptUtil.md5Encrypt(user.getPwd()));
        userService.insert(user);
        session.setAttribute(Constants.SESSION_CUSTOMER, user);
        return "redirect:home";
    }

    @ResponseBody
    @RequestMapping(value = "add", method = RequestMethod.POST)
    public ControllerResult add(User user, HttpSession session) {
        if (SessionUtil.isAdmin(session)) {
            user.setPwd(EncryptUtil.md5Encrypt(user.getPwd()));
            userService.insert(user);
            return ControllerResult.getSuccessResult("成功添加客户信息");
        }
        return ControllerResult.getNotLoginResult("登录信息无效，请重新登录");
    }

    @RequestMapping(value = "home", method = RequestMethod.GET)
    public ModelAndView home(HttpSession session) {
        if (SessionUtil.isUser(session)) {
            User user = (User) session.getAttribute(Constants.SESSION_CUSTOMER);
            ModelAndView mav = new ModelAndView("user/home");
            return mav;
        } else {
            return new ModelAndView("redirect:/index");
        }
    }

    @RequestMapping(value = "list_page", method = RequestMethod.GET)
    public String toListPage(HttpSession session) {
        if (SessionUtil.isAdmin(session)) {
            return "user/users";
        } else {
            return "redirect:/admin/redirect_login_page";
        }
    }

    @RequestMapping(value = "list_page_admin/{type}", method = RequestMethod.GET)
    public String toListPageAdmin(@PathVariable("type") String type, HttpSession session) {
        if (SessionUtil.isAdmin(session)) {
            if (type.equals("res")) {
                return "user/users_res_admin";
            } else if (type.equals("dev")) {
                return "user/users_dev_admin";
            } else if(type.equals("devgroup")) {
                return "user/users_devg_admin";
            } else if (type.equals("pub")) {
                return "user/users_pubplan_admin";
            }
        }
        return "redirect:/admin/redirect_login_page";
    }

    @ResponseBody
    @RequestMapping(value = "search_pager", method = RequestMethod.GET)
    public Pager4EasyUI<User> searchPager(@Param("page")String page, @Param("rows")String rows, User user, HttpSession session) {
        if (SessionUtil.isAdmin(session)) {
            logger.info("show users by pager");
            int total = userService.countByCriteria(user);
            Pager pager = PagerUtil.getPager(page, rows, total);
            List<User> users = userService.queryByPagerAndCriteria(pager, user);
            return new Pager4EasyUI<User>(pager.getTotalRecords(), users);
        } else {
            logger.info("can not show user by pager cause admin is no login");
            return null;
        }
    }

    @RequestMapping(value = "query/{id}", method = RequestMethod.GET)
    public ModelAndView queryById(@PathVariable("id") String id, HttpSession session) {
        if (SessionUtil.isAdmin(session) || SessionUtil.isUser(session)) {
            logger.info("query user info by id: " + id);
            ModelAndView mav = new ModelAndView("user/info");
            User user = userService.queryById(id);
            mav.addObject("user", user);
            return mav;
        }
        return new ModelAndView("redirect:/index");
    }

    @ResponseBody
    @RequestMapping(value = "update", method = RequestMethod.POST)
    public ControllerResult update(User user, HttpSession session) {
        if (SessionUtil.isAdmin(session) || SessionUtil.isUser(session)) {
            logger.info("update user info");
            userService.update(user);
            return ControllerResult.getSuccessResult("成功更新用户信息");
        } else {
            return ControllerResult.getNotLoginResult("登录信息无效，请重新登录");
        }
    }

    @RequestMapping(value = "setting_page", method = RequestMethod.GET)
    public String settingPage(User user, HttpSession session) {
        if (SessionUtil.isUser(session)) {
            return "user/setting";
        } else {
            return "redirect:/redirect_index";
        }
    }

    @ResponseBody
    @RequestMapping(value = "update_pwd", method = RequestMethod.POST)
    public ControllerResult updatePwd(@Param("password")String password, @Param("newPwd")String newPwd, @Param("conPwd")String conPwd, HttpSession session) {
        if (SessionUtil.isUser(session)) {
            User user = (User) session.getAttribute(Constants.SESSION_CUSTOMER);
            if (user.getPwd().equals(EncryptUtil.md5Encrypt(password)) && newPwd != null && conPwd != null && newPwd.equals(conPwd)) {
                user.setPwd(EncryptUtil.md5Encrypt(newPwd));
                userService.updatePassword(user);
                session.setAttribute(Constants.SESSION_CUSTOMER, user);
                return ControllerResult.getSuccessResult("更新用户密码成功");
            } else {
                return ControllerResult.getFailResult("原密码错误,或新密码与确认密码不一致");
            }
        } else {
            return ControllerResult.getNotLoginResult("登录信息无效，请重新登录");
        }
    }

    @ResponseBody
    @RequestMapping(value = "update_other_pwd", method = RequestMethod.POST)
    public ControllerResult updateOtherPwd(User user, HttpSession session) {
        if (SessionUtil.isAdmin(session)) {
            user.setPwd(EncryptUtil.md5Encrypt(user.getPwd()));
            userService.updatePassword(user);
            return ControllerResult.getSuccessResult("更新用户密码成功");
        } else {
            return ControllerResult.getNotLoginResult("登录信息无效，请重新登录");
        }
    }

    @ResponseBody
    @RequestMapping(value = "inactive", method = RequestMethod.GET)
    public ControllerResult inactive(@Param("id")String id, HttpSession session) {
        if (SessionUtil.isAdmin(session)) {
            userService.inactive(id);
            return ControllerResult.getSuccessResult("冻结客户账号成功");
        } else {
            return ControllerResult.getNotLoginResult("登录信息无效，请重新登录");
        }
    }

    @ResponseBody
    @RequestMapping(value = "active", method = RequestMethod.GET)
    public ControllerResult active(@Param("id")String id, HttpSession session) {
        if (SessionUtil.isAdmin(session)) {
            userService.active(id);
            return ControllerResult.getSuccessResult("已解除客户账号冻结");
        } else {
            return ControllerResult.getNotLoginResult("登录信息无效，请重新登录");
        }
    }


    @InitBinder
    public void initBinder(WebDataBinder binder) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }

}
