package com.tanhua.sso.controller;

import com.tanhua.sso.service.SmsService;
import com.tanhua.sso.vo.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("user")
@Slf4j
public class SmsController {

    @Autowired
    private SmsService smsService;

    /**
     * 发送短信验证码接口
     */
    @PostMapping("login")
    public ResponseEntity<ErrorResult> sendCheckCode(@RequestBody Map<String,String> param) {
        ErrorResult errorResult =null;
        String phone = param.get("phone");
        try {
            errorResult = this.smsService.sendCheckCode(phone);
            if (null == errorResult) {
                return ResponseEntity.ok(null);
            }
        } catch (Exception e) {
            log.error("发送短信验证码失败~ phone = "+phone,e);
            ErrorResult.builder().errCode("000002").errMessage("短信验证码发送失败！").build();
            e.printStackTrace();
        }
        //HttpStatus : 枚举，封装了HTTP请求和响应的状态码
        //ResponseEntity : spring提供的一个响应数据实体类，status 方法用来指定响应状态
        //body 方法 ： 指定响应体数据
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);


    }

}
