package com.tanhua.server.interceptor;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanhua.server.utils.Cache;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * 拦截器
 * 进行缓存命中
 */

@Component
public class RedisCacheInterceptor implements HandlerInterceptor {

    @Value("${tanhua.cache.enable}")
    private Boolean enable;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    //类型转换器 json和String之间互相转换
    private static final ObjectMapper MAPPER= new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //缓存的全局开关校验
        if (enable){
            return true;
        }

        //校验handler是否是HandlerMethod
        if (!(handler instanceof HandlerMethod)){
            return true;
        }

        //判断是否为get请求
        if (!((HandlerMethod) handler).hasMethodAnnotation(GetMapping.class)){
            return true;
        }

        //判断是否添加了@Cache注解
        if (!((HandlerMethod) handler).hasMethodAnnotation(Cache.class)) {
            return true;
        }

        //缓存命中
        String redisKey = createRedisKey(request);
        //从redis中拿到指定key的value
        String cacheData = this.redisTemplate.opsForValue().get(redisKey);

        if (StringUtils.isEmpty(cacheData)){
            //缓存未命中
            return true;
        }

        //将data数据进行响应
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset-utf-8");
        response.getWriter().write(cacheData);


        return false;
    }


    /**
     * 生成redis中的key，规则：SERVER_CACHE_DATA_MD5(url + param + token)
     *
     * @param request
     * @return
     */
    static String createRedisKey(HttpServletRequest request) throws JsonProcessingException {
        String url = request.getRequestURI();
        String param = MAPPER.writeValueAsString(request.getParameterMap());
        String token = request.getHeader("Authorization");

        String data = url +"_" + token;

        return "SERVER_CACHE_DATA_" + DigestUtils.md5Hex(data);
    }


}
