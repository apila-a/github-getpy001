package com.tanhua.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanhua.server.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.list.PredicatedList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class UserService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${tanhua.sso.url}")
    private String ssoUrl;

    private static final ObjectMapper MAPPER=new ObjectMapper();

    /**
     * 通过sso的rest接口查询
     *
     * @param token
     * @return
     */

    public User queryUserByToken(String token) {
        String url = ssoUrl + "/user/" +token;
        try {
            String data = this.restTemplate.getForObject(url, String.class);
            if (StringUtils.isEmpty(data)){
                return null;
            }
            return MAPPER.readValue(data, User.class);
        } catch (Exception e) {
            log.error("校验token出错，token = " + token,e);
        }

        return null;
    }
}
