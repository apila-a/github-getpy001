package com.tanhua.dubbo.server.api;

import com.alibaba.dubbo.config.annotation.Service;
import com.tanhua.dubbo.server.pojo.PageInfo;
import com.tanhua.dubbo.server.pojo.RecommendUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;


@Service(version = "1.0.0")
public class RecommendUserApiImpl implements RecommendUserApi {

    @Autowired
    private MongoTemplate mongoTemplate;


    /**
     * "今日佳人"查询
     * @param userId
     * @return
     */
    @Override
    public RecommendUser queryWithMaxScore(Long userId) {
        //查询得分最高的用户，按照得分倒序排序

        Query query=Query.query(Criteria.where("toUserId").is(userId)).with(Sort.by(Sort.Order.desc("score"))).limit(1);
        return this.mongoTemplate.findOne(query, RecommendUser.class);
    }

    /**
     * 查询列表
     * @param userId
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageInfo<RecommendUser> queryPageInfo(Long userId, Integer pageNum, Integer pageSize) {
        //分页并且指定排序参数
        PageRequest pageRequest= PageRequest.of(pageNum-1, pageSize,Sort.by(Sort.Order.desc("score")));
        //查询参数
        Query query = Query.query(Criteria.where("toUserId").is(userId)).with(pageRequest);

        List<RecommendUser> recommendUserList= this.mongoTemplate.find(query, RecommendUser.class);

        return new PageInfo<>(0,pageNum,pageSize,recommendUserList);

    }

    /**
     * 查询推荐好友的缘分值
     *
     * @param userId
     * @param toUserId
     * @return
     */
    @Override
    public double queryScore(Long userId, Long toUserId) {
        Query query = Query.query(Criteria
                .where("toUserId").is(toUserId)
                .and("userId").is(userId));
        RecommendUser recommendUser = this.mongoTemplate.findOne(query, RecommendUser.class);
        if (null == recommendUser) {
            return 0;
        }
        return recommendUser.getScore();
    }
}
