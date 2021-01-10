package com.tanhua.server.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.tanhua.dubbo.server.api.RecommendUserApi;
import com.tanhua.dubbo.server.pojo.PageInfo;
import com.tanhua.dubbo.server.pojo.RecommendUser;
import com.tanhua.server.vo.TodayBest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
public class RecommendUserService {

    @Reference(version = "1.0.0")
    private RecommendUserApi recommendUserApi;

    /**
     * 查询佳人
     *
     * @param userId
     * @return
     */

    public TodayBest queryTodayBest(Long userId) {
        RecommendUser recommendUser = this.recommendUserApi.queryWithMaxScore(userId);
        if (null == recommendUser) {
            return null;
        }
        TodayBest todayBest = new TodayBest();
        todayBest.setId(recommendUser.getUserId());

        //缘分值
        //取整
        double score = Math.floor(recommendUser.getScore());
        todayBest.setFateValue(Double.valueOf(score).longValue());

        return todayBest;
    }

    /**
     * 调用dubbo接口实现查询用户列表
     *
     * @param userId
     * @param page
     * @param pageSize
     * @return
     */
    public PageInfo<RecommendUser> queryRecommendUserList(Long userId, Integer page, Integer pageSize) {
        return this.recommendUserApi.queryPageInfo(userId,page, pageSize);

    }

    /**
     * 查询推荐好友的缘分值
     *
     * @param userId
     * @param toUserId
     * @return
     */
    double queryScore(Long userId, Long toUserId) {
        return this.recommendUserApi.queryScore(userId, toUserId);
    }


}
