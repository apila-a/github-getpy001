package com.tanhua.dubbo.server.api;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.config.annotation.Service;
import com.mongodb.client.result.DeleteResult;
import com.tanhua.dubbo.server.pojo.*;
import com.tanhua.dubbo.server.service.IdService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

@Service(version = "1.0.0")
public class QuanZiApiImpl implements QuanZiApi {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IdService idService;



    @Override
    public String savePublish(Publish publish) {
        //校验
        if (publish.getUserId()==null){
            return null;
        }

        try {
            //设置创建时间
            publish.setCreated(System.currentTimeMillis());
            //设置id
            publish.setId(ObjectId.get());

            //获取自增长Pid，放入publish
            //IdService:自增长id对象 ，
            publish.setPid(this.idService.createId("publish",publish.getId().toHexString()));

            //保存发布
            this.mongoTemplate.save(publish);

            //构建相册对象
            Album album = new Album();
            album.setPublishId(publish.getId());
            album.setCreated(System.currentTimeMillis());
            album.setId(ObjectId.get());
            this.mongoTemplate.save(album, "quanzi_album_"+publish.getUserId());

            //写入好友的时间线中
            //Criteria : TODO
            Criteria criteria = Criteria.where("userId").is(publish.getUserId());

            //查询好友列表
            List<Users> users = this.mongoTemplate.find(Query.query(criteria),  Users.class);

            //遍历好友列表，将动态写入到每个好友的时间线中
            for (Users user : users) {
                TimeLine timeLine = new TimeLine();
                timeLine.setId(ObjectId.get());
                timeLine.setPublishId(publish.getId());
                timeLine.setDate(System.currentTimeMillis());
                timeLine.setUserId(user.getUserId());
                this.mongoTemplate.save(timeLine, "quanzi_time_line_"+user.getFriendId());
            }
            return publish.getId().toHexString();
        } catch (Exception e) {
            e.printStackTrace();
            //TODO 出错的事务回滚，MongoDB非集群不支持事务，暂不进行实现
        }

        return null;


    }

    /**
     * 查询动态实现
     *
     * @param userId
     * @param page
     * @param pageSize
     * @return
     */

    @Override
    public PageInfo<Publish> queryPublishList(Long userId, Integer page, Integer pageSize) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("date")));
        Query query = new Query().with(pageable);

        String tableName = "quanzi_time_line_";
        if (null == userId) {
            //查询推荐动态
            tableName += "recommend";
        } else {
            //查询好友动态
            tableName += userId;
        }

        // 查询自己的时间线表
        List<TimeLine> timeLines = this.mongoTemplate.find(query, TimeLine.class, tableName);

        List<ObjectId> ids = new ArrayList<>();
        for (TimeLine timeLine : timeLines) {
            ids.add(timeLine.getPublishId());
        }

        Query queryPublish = Query.query(Criteria.where("id").in(ids)).with(Sort.by(Sort.Order.desc("created")));
        //查询动态信息
        List<Publish> publishList = this.mongoTemplate.find(queryPublish, Publish.class);

        // 封装分页对象
        PageInfo<Publish> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(page);
        pageInfo.setPageSize(pageSize);
        pageInfo.setTotal(0); //不提供总数
        pageInfo.setRecords(publishList);

        return pageInfo;
    }

    /**
     * 点赞
     *
     * @param userId
     * @param publishId
     * @return
     */

    @Override
    public boolean saveLikeComment(Long userId, String publishId) {
        Query query = Query.query(Criteria.where("publishId")
                .is(new ObjectId(publishId)).and("userId")
                .is(userId).and("commentType").is(1));
        long count = this.mongoTemplate.count(query,Comment.class);
        if (count>0){
            return false;
        }

        return this.saveComment(userId,publishId, 1,null);
    }

    /**
     * 取消点赞/喜欢
     *
     * @param userId
     * @param publishId
     * @param commentType
     * @return
     */

    @Override
    public boolean removeComment(Long userId, String publishId, Integer commentType) {
        Query query = Query.query(Criteria
                .where("publishId").is(new ObjectId(publishId))
                .and("userId").is(userId)
                .and("commentType").is(commentType));
        DeleteResult remove = this.mongoTemplate.remove(query, Comment.class);

        return remove.getDeletedCount()>0;
    }

    /**
     * 喜欢
     * @param userId
     * @param publishId
     * @return
     */

    @Override
    public boolean saveLoveComment(Long userId, String publishId) {
        Query query = Query.query(Criteria.where("publishId")
                .is(new ObjectId(publishId)).and("userId")
                .is(userId).and("commentType").is(3));
        long count = this.mongoTemplate.count(query,Comment.class);
        if (count>0){
            return false;
        }

        return this.saveComment(userId,publishId, 3,null);
    }

    /**
     * 保存评论
     *
     * @param userId
     * @param publishId
     * @param type
     * @param content
     * @return
     */

    @Override
    public boolean saveComment(Long userId, String publishId, Integer type, String content) {
        try {
            Comment comment = new Comment();
            comment.setId(ObjectId.get());
            comment.setUserId(userId);
            comment.setPublishId(new ObjectId(publishId));
            comment.setContent(content);
            comment.setCommentType(type);
            comment.setCreated(System.currentTimeMillis());

            // 设置发布人的id
            Publish publish = this.mongoTemplate.findById(comment.getPublishId(), Publish.class);
            if (null != publish) {
                comment.setPublishUserId(publish.getUserId());
            } else {
                Video video = this.mongoTemplate.findById(comment.getPublishId(), Video.class);
                if (null != video) {
                    comment.setPublishUserId(video.getUserId());
                }
            }

            this.mongoTemplate.save(comment);

            return true;
        } catch (Exception e){
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 查询评论数
     *
     * @param publishId
     * @param type
     * @return
     */
    @Override
    public Long queryCommentCount(String publishId, Integer type) {
        Query query = Query.query(Criteria.where("publishId").is(publishId).and("commentType").is(type));

        return this.mongoTemplate.count(query,Comment.class);
    }

    /**
     * 查询评论
     * @param publishId
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public PageInfo<Comment> queryCommentList(String publishId, Integer page, Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.asc("created")));
        Query query = new Query(Criteria
                .where("publishId").is(new ObjectId(publishId))
                .and("commentType").is(2))
                .with(pageRequest);

        //查询时间线表
        List<Comment> timeLineList = this.mongoTemplate.find(query, Comment.class);

        PageInfo<Comment> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(page);
        pageInfo.setPageSize(pageSize);
        pageInfo.setRecords(timeLineList);
        pageInfo.setTotal(0); //不提供总数
        return pageInfo;
    }

    /**
     * 根据id查询动态
     *
     * @param id
     * @return
     */
    @Override
    public Publish queryPublishById(String id) {
        return this.mongoTemplate.findById(id, Publish.class);
    }

    /**
     * 查询用户评论数据
     *
     * @param userId
     * @param type
     * @param page
     * @param pageSize
     * @return
     */

    @Override
    public PageInfo<Comment> queryCommentListByUser(Long userId, Integer type, Integer page, Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1 ,pageSize,Sort.by(Sort.Order.desc("created")));
        Query query = Query.query(Criteria
                .where("publishUserId").is(userId)
                .and("commentType").is(type))
                .with(pageRequest);

        //Comment 评论表
        List<Comment> commentsList = this.mongoTemplate.find(query, Comment.class);

        PageInfo<Comment> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(page);
        pageInfo.setPageSize(pageSize);
        pageInfo.setRecords(commentsList);
        pageInfo.setTotal(0);

        return pageInfo;
    }

    @Override
    public List<Publish> queryPublishByPids(List<Long> pids) {
        Query query = Query.query(Criteria.where("pid").in(pids));
        return this.mongoTemplate.find(query, Publish.class);
    }


    //QuanZiApiImpl
    @Override
    public PageInfo<Publish> queryAlbumList(Long userId, Integer page, Integer pageSize) {
        PageInfo<Publish> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(page);
        pageInfo.setPageSize(pageSize);
        pageInfo.setTotal(0); //不提供总数

        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("created")));
        Query query = new Query().with(pageRequest);
        List<Album> albumList = this.mongoTemplate.find(query, Album.class, "quanzi_album_" + userId);

        if(CollectionUtils.isEmpty(albumList)){
            return pageInfo;
        }


        List<ObjectId> publishIds = new ArrayList<>();
        for (Album album : albumList) {
            publishIds.add(album.getPublishId());
        }

        //查询发布信息
        Query queryPublish = Query.query(Criteria.where("id").in(publishIds)).with(Sort.by(Sort.Order.desc("created")));
        List<Publish> publishList = this.mongoTemplate.find(queryPublish, Publish.class);

        pageInfo.setRecords(publishList);

        return pageInfo;
    }
}






















