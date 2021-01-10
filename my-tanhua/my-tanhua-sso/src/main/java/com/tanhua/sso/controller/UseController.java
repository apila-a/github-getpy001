package com.tanhua.sso.controller;

import com.tanhua.sso.pojo.User;
import com.tanhua.sso.service.UserService;
import com.tanhua.sso.vo.ErrorResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.protocol.ResponseServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("user")
public class UseController {

    @Autowired
    private UserService userService;

    @PostMapping("loginVerification")
    public ResponseEntity<Object> login(@RequestBody Map<String,String> param){
        try {
            String phone= param.get("phone");
            String code = param.get("verificationCode");
            String data = this.userService.login(phone, code);
            if (StringUtils.isNotEmpty(data)){
                //登陆成功
                Map<String,Object> result = new HashMap<>(2);
                String[] ss=StringUtils.split(data,"|");
                result.put("token",ss[0]);
                result.put("isNew",Boolean.valueOf(ss[1]));
                return  ResponseEntity.ok(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ErrorResult errorResult=ErrorResult.builder().errCode("000002").errMessage("登陆失败！").build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
    }

    /**
     * 校验token，根据token查询用户数据
     *
     * @param token
     * @return
     */
    @GetMapping("{token}")
    public User queryUserByToken(@PathVariable("token") String token) {
        return this.userService.queryUserByToken(token);
    }


}
