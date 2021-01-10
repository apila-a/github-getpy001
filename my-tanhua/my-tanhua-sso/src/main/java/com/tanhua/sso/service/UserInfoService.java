package com.tanhua.sso.service;

import com.alibaba.druid.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tanhua.sso.enums.SexEnum;
import com.tanhua.sso.mapper.UserInfoMapper;
import com.tanhua.sso.pojo.User;
import com.tanhua.sso.pojo.UserInfo;
import com.tanhua.sso.vo.PicUploadResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class UserInfoService {
    @Autowired
    private UserService userService;

    @Autowired
    private UserInfoMapper userInfoMapper;

    //人脸识别
    /*@Autowired
    private FaceEngineService faceEngineService;*/

    @Autowired
    private PicUploadService picUploadService;

    public Boolean saveUserInfo(Map<String, String> param,String token){
        //校验token
        User user = this.userService.queryUserByToken(token);
        if (null==user){
            return false;
        }
        UserInfo userInfo =new UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setSex(StringUtils.equalsIgnoreCase(param.get("gender"), "man")? SexEnum.MAN:SexEnum.WOMAN);
        userInfo.setNickName(param.get("nickname"));
        userInfo.setBirthday(param.get("birthday"));
        userInfo.setCity(param.get("city"));
        return this.userInfoMapper.insert(userInfo)==1;

    }

    public Boolean saveUserLogo(MultipartFile file, String token) {
        //校验token
        User user = this.userService.queryUserByToken(token);
        if (null == user) {
            return false;
        }

        /*try {
            //校验图片是否是人像，如果不是人像就返回false
            boolean b = this.faceEngineService.checkIsPortrait(file.getBytes());
            if (!b) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        //图片上传到阿里云OSS
        PicUploadResult result = this.picUploadService.upload(file);
        if (StringUtils.isEmpty(result.getName())) {
            //上传失败
            return false;
        }

        //把头像保存到用户信息表中
        UserInfo userInfo = new UserInfo();
        userInfo.setLogo(result.getName());

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", user.getId());

        return this.userInfoMapper.update(userInfo, queryWrapper) == 1;
    }

}

