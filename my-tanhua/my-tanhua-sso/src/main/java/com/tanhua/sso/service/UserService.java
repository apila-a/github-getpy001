package com.tanhua.sso.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanhua.sso.mapper.UserMapper;
import com.tanhua.sso.pojo.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.hash.HashMapper;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Value("${jwt.secret}")
    private String secret;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private HuanXinService huanXinService;

    /**
     * 登录逻辑
     *
     * @param
     * @param code
     * @return 如果校验成功返回token，失败返回null
     */
   /* public String login(String mobile, String code) {
        //校验验证码是否正确
        String redisKey = "CHECK_CODE_" + mobile;
        String value = this.redisTemplate.opsForValue().get(redisKey);
        if (StringUtils.isEmpty(value)) {
            //验证码失效
            return null;
        }

        if (!StringUtils.equals(value, code)) {
            // 验证码输入错误
            return null;
        }

        Boolean isNew = false; //默认是已注册

        //校验该手机号是否已经注册，如果没有注册，需要注册一个账号，如果已经注册，直接登录
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mobile", mobile);
        User user = this.userMapper.selectOne(queryWrapper);
        if (null == user) {
            // 该手机号未注册
            user = new User();
            user.setMobile(mobile);
            // 默认密码
            user.setPassword(DigestUtils.md5Hex("123456"));
            this.userMapper.insert(user);

            isNew = true;

            //注册用户到环信平台
            this.huanXinService.register(user.getId());
        }

        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("mobile", mobile);
        claims.put("id", user.getId());

        // 生成token
        String token = Jwts.builder()
                .setClaims(claims) //设置响应数据体
                .signWith(SignatureAlgorithm.HS256, secret) //设置加密方法和加密盐
                .compact();

        try {
            // 将token存储到redis中
            String redisTokenKey = "TOKEN_" + token;
            String redisTokenValue = MAPPER.writeValueAsString(user);
            this.redisTemplate.opsForValue().set(redisTokenKey, redisTokenValue, Duration.ofHours(1));
        } catch (Exception e) {
            LOGGER.error("存储token出错", e);
            return null;
        }

        try {
            //发送消息
            Map<String, Object> msg = new HashMap<>();
            msg.put("id", user.getId());
            msg.put("mobile", mobile);
            msg.put("date", new Date());
            this.rocketMQTemplate.convertAndSend("tanhua-sso-login", msg);
        } catch (Exception e) {
            LOGGER.error("发送消息出错", e);
        }

        return isNew + "|" + token;
    }*/

    public String login(String phone, String code) {
        String redisKey = "CHECK_CODE_" + phone;
        boolean isNew = false;

        //校验验证码
        String redisData = this.redisTemplate.opsForValue().get(redisKey);
        if (!StringUtils.equals(code, redisData)){
            return null; //验证码错误
        }

        //验证码在校验完成后，需要废弃
        this.redisTemplate.delete(redisKey);


        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("mobile", phone);
        User user = this.userMapper.selectOne(queryWrapper);

        if (null==user){
            //需要注册该用户
            user = new User();
            user.setMobile(phone);
            user.setPassword(DigestUtils.md5Hex("123456"));

            //注册用户
            this.userMapper.insert(user);
            isNew=true;

            //this.huanXinService.register(user.getId());
        }


        //this.huanXinService.register(user.getId());

        //生成token
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("mobile",phone);
        claims.put("id",user.getId());

        String token = Jwts.builder()
                .setClaims(claims)                      //payload, 存放数据的位置，不能放置敏感数据
                .signWith(SignatureAlgorithm.HS256, secret)         //设置加密方法和盐值
                .setExpiration(new DateTime().plusHours(12).toDate())   //设置过期时间
                .compact();

        try {
            Map<String,Object> msg= new HashMap<>();
            msg.put("id",user.getId());
            msg.put("mobile",phone);
            msg.put("date",System.currentTimeMillis());

            //this.rocketMQTemplate.syncSend("tanhua-sso-login", msg, 8000);

            this.rocketMQTemplate.convertAndSend("tanhua-sso-login",msg);

        } catch (MessagingException e) {
            log.error("发送失败！",e);
        }
        return token + "|"+isNew;


    }

    public User queryUserByToken(String token) {
        try {
            // 通过token解析数据
            Map<String, Object> body = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();

            User user = new User();
            user.setId(Long.valueOf(body.get("id").toString()));

            //需要返回user对象中的mobile，需要查询数据库获取到mobile数据
            //如果每次都查询数据库，必然会导致性能问题，需要对用户的手机号进行缓存操作
            //数据缓存时，需要设置过期时间，过期时间要与token的时间一致
            //如果用户修改了手机号，需要同步修改redis中的数据

            String redisKey = "TANHUA_USER_MOBILE_" + user.getId();
            if(this.redisTemplate.hasKey(redisKey)){
                String mobile = this.redisTemplate.opsForValue().get(redisKey);
                user.setMobile(mobile);
            }else {
                //查询数据库
                User u = this.userMapper.selectById(user.getId());
                user.setMobile(u.getMobile());

                //将手机号写入到redis中
                //在jwt中的过期时间的单位为：秒
                long timeout = Long.valueOf(body.get("exp").toString()) * 1000 - System.currentTimeMillis();
                this.redisTemplate.opsForValue().set(redisKey, u.getMobile(), timeout, TimeUnit.MILLISECONDS);
            }

            return user;
        } catch (ExpiredJwtException e) {
            log.info("token已经过期！ token = " + token);
        } catch (Exception e) {
            log.error("token不合法！ token = "+ token, e);
        }
        return null;
    }

}
