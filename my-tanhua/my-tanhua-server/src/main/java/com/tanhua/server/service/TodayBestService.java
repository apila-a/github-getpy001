package com.tanhua.server.service;


import com.alibaba.dubbo.config.annotation.Reference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanhua.dubbo.server.api.RecommendUserApi;
import com.tanhua.dubbo.server.api.UserLocationApi;
import com.tanhua.dubbo.server.pojo.PageInfo;
import com.tanhua.dubbo.server.pojo.RecommendUser;
import com.tanhua.dubbo.server.vo.UserLocationVo;
import com.tanhua.server.enums.SexEnum;
import com.tanhua.server.pojo.Question;
import com.tanhua.server.pojo.User;
import com.tanhua.server.pojo.UserInfo;
import com.tanhua.server.utils.UserThreadLocal;
import com.tanhua.server.vo.NearUserVo;
import com.tanhua.server.vo.PageResult;
import com.tanhua.server.vo.RecommendUserQueryParam;
import com.tanhua.server.vo.TodayBest;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.parser.HttpParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class TodayBestService {

    @Autowired
    private UserLocationApi userLocationApi;

    @Autowired
    private UserService userService;

    @Autowired
    private RecommendUserService recommendUserService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private RestTemplate restTemplate;



    private static final ObjectMapper Mapper = new ObjectMapper();

    @Value("${tanhua.huanxin.url}")
    private String url;

    @Value("${tanhua.sso.default.user}")
    private Long defaultUser;

    @Value("${tanhua.sso.default.recommend.users}")
    private String defaultRecommendUsers;

    public TodayBest queryTodayBest() {
        //校验token是否有效，通过sso的接口进行校验

        User user = UserThreadLocal.get();

        //查询推荐用户（今日佳人）
        TodayBest todayBest = this.recommendUserService.queryTodayBest(user.getId());
        if (null == todayBest) {
            //给出默认推荐
            todayBest = new TodayBest();
            todayBest.setId(defaultUser);
            todayBest.setFateValue(80L);//固定值
        }

        //补全个人信息
        UserInfo userInfo = this.userInfoService.queryUserInfoByUserId(todayBest.getId());
        if (null == userInfo) {
            return null;
        }
        todayBest.setAvatar(userInfo.getLogo());
        todayBest.setNickname(userInfo.getNickName());
        todayBest.setTags(StringUtils.split(userInfo.getTags(), ","));
        todayBest.setGender(userInfo.getSex().getValue() == 1 ? "man" : "woman");
        todayBest.setAge(userInfo.getAge());

        return todayBest;


    }

    /**
     * 重载上面的方法
     *
     * @param userId
     * @return
     */

    public TodayBest queryTodayBest(Long userId) {
        //校验token是否有效，通过sso的接口进行校验

        User user = UserThreadLocal.get();

        TodayBest todayBest = new TodayBest();
        //补全信息
        UserInfo userInfo = this.userInfoService.queryUserInfoByUserId(userId);
        todayBest.setId(userId);
        todayBest.setAge(userInfo.getAge());
        todayBest.setAvatar(userInfo.getLogo());
        todayBest.setGender(userInfo.getSex().name().toLowerCase());
        todayBest.setNickname(userInfo.getNickName());
        todayBest.setTags(StringUtils.split(userInfo.getTags(), ','));

        double score = this.recommendUserService.queryScore(userId, user.getId());
        if(score == 0){
            score = 98; //默认分值
        }

        todayBest.setFateValue(Double.valueOf(score).longValue());

        return todayBest;


    }

    /**
     * 查询用户列表
     *
     * @para     前端过来的token
     * @param queryParam 查询参数对象
     * @return
     */

    public PageResult queryRecommendation(RecommendUserQueryParam queryParam) {
        //获得当前登陆信息
        User user = UserThreadLocal.get();

        //创建一个结果对象
        PageResult pageResult = new  PageResult();

        pageResult.setCounts(0); //前端不参与计算，仅需要返回字段

        pageResult.setPage(queryParam.getPage());
        pageResult.setPageSize(queryParam.getPagesize());

        //调用RecommendUserService对象中的方法得到用户列表
        PageInfo<RecommendUser> pageInfo = this.recommendUserService.queryRecommendUserList(user.getId(),queryParam.getPage(),queryParam.getPagesize());

        //获取数据列表records
        List<RecommendUser> records = pageInfo.getRecords();

        //使用CollectionUtils的isEmpty方法判断records是否为空
        if (CollectionUtils.isEmpty(records)){
            //使用默认推荐列表
            String[] ss=StringUtils.split(defaultRecommendUsers, ',');

            for (String s : ss) {
                RecommendUser recommendUser = new RecommendUser();

                recommendUser.setUserId(Long.valueOf(s));
                recommendUser.setToUserId(user.getId());
                recommendUser.setScore(RandomUtils.nextDouble(70, 99));

                records.add(recommendUser);
            }
        }

        //填充个人信息

        //收集推荐的用户id
        Set<Long> userIds = new HashSet<>();
        for (RecommendUser record : records) {
            userIds.add(record.getUserId());
        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();

        //用户id参数
        queryWrapper.in("user_id", userIds);

        if (StringUtils.isNotEmpty(queryParam.getGender())){
            //需要性别参数查询
            //queryWrapper.eq("sex", StringUtils.equals(queryParam.getGender(), "man") ? 1 : 2);
        }

        if (StringUtils.isNotEmpty(queryParam.getCity())) {
            //需要城市参数查询
            //queryWrapper.like("city", queryParam.getCity());
        }

        if (queryParam.getAge()!=null){
            //设置年龄参数，条件：小于等于
            //queryWrapper.le("age", queryParam.getAge());
        }

        //调用UserInfoService 的queryUserInfoList方法查询推荐用户列表
        List<UserInfo> userInfosList = this.userInfoService.queryUserInfoList(queryWrapper);

        if (CollectionUtils.isEmpty(userInfosList)){
            //没有查询到用户的基本信息
            return pageResult;
        }

        //新建集合存放查询出来的用户列表
        List<TodayBest> todayBests = new ArrayList<>();

        for (UserInfo userInfo : userInfosList) {
            //单独接收没条数据
            TodayBest todayBest = new TodayBest();

            todayBest.setId(userInfo.getId());
            todayBest.setAvatar(userInfo.getLogo());
            todayBest.setNickname(userInfo.getNickName());
            todayBest.setGender(userInfo.getSex().getValue()==1?"man":"woman");
            todayBest.setAge(userInfo.getAge());
            todayBest.setTags(StringUtils.split(userInfo.getTags(), ","));

            //从数据列表中查找和当前遍历用户id相等的用户，取出缘分值score做取整操作
            for (RecommendUser record : records) {
                if (record.getUserId().longValue()==userInfo.getUserId().longValue()){
                    double score = Math.floor(record.getScore()); //取整
                    //将score转换成包装类。然后转化为Long，最后放入todayBest
                    todayBest.setFateValue(Double.valueOf(score).longValue());
                    break;
                }
            }

            //将包装好的每个todayBest对象存入todayBests集合
            todayBests.add(todayBest);

        }

        //按照缘分值进行倒序排序

        Collections.sort(todayBests, (o1,o2) -> new Long(o2.getFateValue()-o1.getFateValue()).intValue());

        //将集合封装排序好的用户集合放入结果集对象
        pageResult.setItems(todayBests);

        return pageResult;
    }

    public String queryQuestion(Long userId) {
        Question question = this.questionService.queryQuestion(userId);
        if (null != question) {
            return question.getTxt();
        }
        return "";
    }

    /**
     * 回复陌生人问题，发送消息给对方
     *
     * @param userId
     * @param reply
     * @return
     */
    public Boolean replyQuestion(Long userId, String reply) {
        User user = UserThreadLocal.get();
        UserInfo userInfo = this.userInfoService.queryUserInfoByUserId(user.getId());

        //构建消息内容
        Map<String, Object> msg = new HashMap<>();
        msg.put("userId", user.getId().toString());
        msg.put("nickname", userInfo.getNickName());
        msg.put("strangerQuestion", this.queryQuestion(userId));
        msg.put("reply", reply);

        try {
            String msgStr = Mapper.writeValueAsString(msg);

            String targetUrl = this.url + "/user/huanxin/messages";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("target", userId.toString());
            params.add("msg", msgStr);

            HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(params, headers);

            ResponseEntity<Void> responseEntity = this.restTemplate.postForEntity(targetUrl, httpEntity, Void.class);

            return responseEntity.getStatusCodeValue() == 200;
        } catch (Exception e) {
            e.printStackTrace();
        }


        return false;
    }

    /**
     * 搜附近
     * @param gender
     * @param distance
     * @return
     */
    public List<NearUserVo> queryNearUser(String gender, String distance) {
        User user = UserThreadLocal.get();
        //查询当前用户的位置信息
        UserLocationVo userLocationVo= this.userLocationApi.queryByUserId(user.getId());

        //获取经纬度
        Double longitude = userLocationVo.getLongitude();
        Double latitude = userLocationVo.getLatitude();

        //根据当前用户的位置信息查询附近的好友
        List<UserLocationVo> userLocationVoList = this.userLocationApi.queryUserFromLocation(longitude,latitude,Integer.valueOf(distance));

        if (CollectionUtils.isEmpty(userLocationVoList)){
            //返回生成的空集合
            return Collections.emptyList();
        }

        List<Long> userIds = new ArrayList<>();
        for (UserLocationVo locationVo : userLocationVoList) {
            userIds.add(locationVo.getUserId());
        }

        //设置查询条件
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id",userIds);
        //equalsIgnoreCase 忽略大小写
        if (StringUtils.equalsIgnoreCase(gender, "man")){
            queryWrapper.in("sex",SexEnum.MAN);
        }else if (StringUtils.equalsIgnoreCase(gender, "woman")){
            queryWrapper.in("sex",SexEnum.WOMAN);
        }

        //拿到用户符合条件的用户集合
        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoList(queryWrapper);

        List<NearUserVo> nearUserVoList = new ArrayList<>();

        for (UserLocationVo locationVo : userLocationVoList) {
            //排除自己
            if (locationVo.getUserId().longValue() == user.getId().longValue()) {
                //跳过本次循环
                continue;
            }
            for (UserInfo userInfo : userInfoList) {
                if (locationVo.getUserId().longValue() == userInfo.getUserId().longValue()) {
                    NearUserVo nearUserVo = new NearUserVo();
                    nearUserVo.setUserId(userInfo.getUserId());
                    nearUserVo.setAvatar(userInfo.getLogo());
                    nearUserVo.setNickname(userInfo.getNickName());
                    nearUserVoList.add(nearUserVo);
                    break;
                }
            }
        }

        return nearUserVoList;
    }
}



















