package com.tanhua.dubbo.server.api;

import com.alibaba.dubbo.config.annotation.Service;
import com.tanhua.dubbo.server.pojo.RecommendUser;
import com.tanhua.dubbo.server.pojo.Visitors;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.awt.print.Pageable;
import java.util.List;

@Service(version = "1.0.0")
public class VisitorsApiImpl implements VisitorsApi {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 保存访客记录
     *
     * @param visitors
     * @return
     */
    @Override
    public String saveVisitor(Visitors visitors) {
        visitors.setId(ObjectId.get());
        visitors.setDate(System.currentTimeMillis());
        this.mongoTemplate.save(visitors);

        return visitors.getId().toHexString();
    }

    /**
     *  查询最近访客
     *
     * @param userId 用户id
     * @param num   查询的数量
     * @return
     */
    @Override
    public List<Visitors> topVisitor(Long userId, Integer num) {
        PageRequest pageRequest = PageRequest.of(0, num, Sort.by(Sort.Order.desc("date")));
        Query query = Query.query(Criteria.where("userId").is(userId)).with(pageRequest);
        return this.queryVisitorList(query);
    }

    /**
     * 按照时间倒序排序，查询最近的访客信息
     *
     * @param userId 用户ID
     * @param date  时间限制，某个时间之后的访问数据
     * @return
     */
    @Override
    public List<Visitors> topVisitor(Long userId, Long date) {
        Query query = Query.query(Criteria
                .where("userId").is(userId)
                .and("date").lt(date));
        return this.queryVisitorList(query);
    }

    /**
     * 统一抽取的方法
     * @param query
     * @return
     */
    private List<Visitors> queryVisitorList(Query query) {
        List<Visitors> visitors = this.mongoTemplate.find(query, Visitors.class);

        // 查询得分 根据每个来访者的信息查询其推荐的得分
        for (Visitors visitor : visitors) {
            Query queryRecommend = Query.query(Criteria
                    .where("toUserId").is(visitor.getUserId())
                    .and("userId").is(visitor.getVisitorUserId()));
            RecommendUser recommendUser = this.mongoTemplate.findOne(queryRecommend, RecommendUser.class);
            if (null != recommendUser) {
                visitor.setScore(recommendUser.getScore());
            } else {
                visitor.setScore(30d);
            }
        }

        return visitors;
    }
}
