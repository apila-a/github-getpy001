package com.tanhua.server.service;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.annotation.Reference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tanhua.dubbo.server.api.QuanZiApi;
import com.tanhua.dubbo.server.pojo.Comment;
import com.tanhua.dubbo.server.pojo.PageInfo;
import com.tanhua.server.pojo.User;
import com.tanhua.server.pojo.UserInfo;
import com.tanhua.server.utils.UserThreadLocal;
import com.tanhua.server.vo.Comments;
import com.tanhua.server.vo.PageResult;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

@Service
public class CommentsService {

    @Reference(version = "1.0.0")
    private QuanZiApi quanZiApi;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    /**
     * 查询评论列表
     *
     * @param publishId
     * @param page
     * @param pageSize
     * @return
     */
    public PageResult queryCommentsList(String publishId, Integer page, Integer pageSize) {
        User user = UserThreadLocal.get();

        PageInfo<Comment> pageInfo = this.quanZiApi.queryCommentList(publishId,page, pageSize);

        //records 数据列表
        List<Comment> records = pageInfo.getRecords();

        //判断是否为空
        if (records.isEmpty()) {
            PageResult pageResult = new PageResult();
            pageResult.setPage(page);
            pageResult.setPageSize(pageSize);
            pageResult.setPages(0);
            pageResult.setCounts(0);
            return pageResult;
        }

        List<Long> userIds = new ArrayList<>();
        for (Comment comment : records) {
            //List的contains : 判断集合中是否包含这个对象
            //此处为了防止重复
            if (!userIds.contains(comment.getUserId())){
                userIds.add(comment.getUserId());
            }
        }
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id",userIds);
        List<UserInfo> userInfos = this.userInfoService.queryUserInfoList(queryWrapper);

        List<Comments> result = new ArrayList<>();
        for (Comment record : records) {
            Comments comments = new Comments();
            comments.setContent(record.getContent());
            comments.setCreateDate(new DateTime(record.getCreated()).toString("yyyy年MM月dd日 HH:mm"));
            comments.setId(record.getId().toHexString());

            for (UserInfo userInfo : userInfos) {
                if (record.getUserId().longValue()==userInfo.getUserId().longValue()){
                    comments.setAvatar(userInfo.getLogo());
                    comments.setNickname(userInfo.getNickName());
                    break;
                }
            }

            String key = "QUANZI_COMMENT_LIKE_" + comments.getId();
            String value= this.redisTemplate.opsForValue().get(key);
            if (StringUtils.isNotEmpty(value)){
                comments.setLikeCount(Integer.valueOf(value));//点赞数
            }else {
                comments.setLikeCount(0);
            }

            String userKey =  "QUANZI_COMMENT_LIKE_USER_" + user.getId() + "_"+comments.getId();
            comments.setHasLiked(this.redisTemplate.hasKey(userKey)?1:0);//是否喜欢 （1是0否）
            result.add(comments);
        }

        PageResult pageResult = new PageResult();
        pageResult.setItems(result);
        pageResult.setPage(page);
        pageResult.setPageSize(pageSize);
        pageResult.setPages(0);
        pageResult.setCounts(0);

        return pageResult;

    }

    /**
     * 保存评论
     *
     * @param publishId
     * @param content
     * @return
     */
    public Boolean saveComments(String publishId, String content) {
        User user = UserThreadLocal.get();
        return this.quanZiApi.saveComment(user.getId(),publishId,2,content);
    }
}
