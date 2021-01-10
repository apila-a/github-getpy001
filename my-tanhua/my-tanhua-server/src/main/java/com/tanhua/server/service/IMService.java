package com.tanhua.server.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baomidou.mybatisplus.core.conditions.query.Query;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sun.codemodel.internal.JCommentPart;
import com.sun.corba.se.pept.transport.ContactInfo;
import com.tanhua.dubbo.server.api.QuanZiApi;
import com.tanhua.dubbo.server.api.UsersApi;
import com.tanhua.dubbo.server.pojo.Comment;
import com.tanhua.dubbo.server.pojo.PageInfo;
import com.tanhua.dubbo.server.pojo.Users;
import com.tanhua.server.pojo.Announcement;
import com.tanhua.server.pojo.User;
import com.tanhua.server.pojo.UserInfo;
import com.tanhua.server.utils.UserThreadLocal;
import com.tanhua.server.vo.Contacts;
import com.tanhua.server.vo.MessageAnnouncement;
import com.tanhua.server.vo.MessageLike;
import com.tanhua.server.vo.PageResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.annotation.Contract;
import org.hibernate.validator.internal.util.Contracts;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.lang.model.type.ArrayType;
import java.util.ArrayList;
import java.util.List;

@Service
public class IMService {

    @Reference(version = "1.0.0")
    private UsersApi usersApi;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${tanhua.sso.url}")
    private String url;

    @Autowired
    private UserInfoService userInfoService;

    @Reference(version = "1.0.0")
    private QuanZiApi quanZiApi;

    @Autowired
    private AnnouncementService announcementService;

    /**
     * 添加好友
     *
     * @param userId
     */
    public boolean contactUser(Long userId) {
        User user = UserThreadLocal.get();

        Users users = new Users();
        users.setUserId(user.getId());
        users.setFriendId(userId);

        String id = this.usersApi.saveUsers(users);
        if (StringUtils.isNotEmpty(id)) {
            //注册好友关系到环信
            String targetUrl = url + "/user/huanxin/contacts/" + users.getUserId() + "/" + users.getFriendId();
            ResponseEntity<Void> responseEntity = this.restTemplate.postForEntity(targetUrl, null, Void.class);
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                return true;
            }
            return false;
        }

        return false;
    }

    /**
     * 查询联系人列表
     * @param page
     * @param pageSize
     * @param keyword
     * @return
     */
    public PageResult queryContactsList(Integer page, Integer pageSize, String keyword) {
        User user = UserThreadLocal.get();

        List<Users> usersList = null;
        if (StringUtils.isNotEmpty(keyword)){
            //关键字不为空，查询所有好友，在后面进行关键字过滤
            usersList = this.usersApi.queryAllUsersList(user.getId()); //TODO 此方法未完善
        }else {
            //关键字为空，进行分页查询
            PageInfo<Users> usersPageInfo = this.usersApi.queryUsersList(user.getId(), page ,pageSize);
            usersList= usersPageInfo.getRecords();
        }
        List<Long> userIds = new ArrayList<>();
        for (Users users : usersList) {
            //获得所有好友id
            userIds.add(users.getFriendId());
        }
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id",userIds);
        if (StringUtils.isNotEmpty(keyword)){
            //like模糊查询
            queryWrapper.like("nice_name",keyword);
        }
        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoList(queryWrapper);

        List<Contacts> contactsList = new ArrayList<>();

        //填充用户信息
        for (UserInfo userInfo : userInfoList) {
            Contacts contacts = new Contacts();
            contacts.setAge(userInfo.getAge());
            contacts.setAvatar(userInfo.getLogo());
            contacts.setGender(userInfo.getSex().name().toLowerCase());
            contacts.setUserId(String.valueOf(userInfo.getUserId()) );
            contacts.setNickname(userInfo.getNickName());
            contacts.setCity(StringUtils.substringBefore(userInfo.getCity(),"-"));
            contactsList.add(contacts);

        }

        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPages(0);
        pageResult.setCounts(0);
        pageResult.setPages(pageSize);
        pageResult.setItems(contactsList);

        return pageResult;
    }


    /**
     * 查询点赞 1
     *
     * @param page
     * @param pageSize
     * @return
     */
    public PageResult queryMessageLikeList(Integer page, Integer pageSize) {

        return this.messageCommentList(1,page,pageSize);
    }


    /**
     * 查询评论 2
     * @param page
     * @param pageSize
     * @return
     */
    public PageResult queryMessageCommentList(Integer page, Integer pageSize) {
        return this.messageCommentList( 2, page, pageSize);
    }

    /**
     * 查询喜欢 3
     *
     * @param page
     * @param pageSize
     * @return
     */
    public PageResult queryMessageLoveList(Integer page, Integer pageSize) {
        return  this.messageCommentList(3,page,pageSize);
    }


    /**
     * 将查询评论功能统一抽取
     * 
     * @param type
     * @param page
     * @param pageSize
     * @return
     */

    private PageResult messageCommentList(Integer type, Integer page, Integer pageSize) {
        User user = UserThreadLocal.get();
        PageInfo<Comment> pageInfo = this.quanZiApi.queryCommentListByUser(user.getId(), type, page, pageSize);

        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPages(0);
        pageResult.setCounts(0);
        pageResult.setPageSize(pageSize);

        List<Comment> records = pageInfo.getRecords();

        List<Long> userIds = new ArrayList<>();
        for (Comment comment : records) {
            userIds.add(comment.getUserId());
        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", userIds);
        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoList(queryWrapper);

        List<MessageLike> messageLikeList = new ArrayList<>();
        for (Comment record : records) {
            for (UserInfo userInfo : userInfoList) {
                if (userInfo.getUserId().longValue() == record.getUserId().longValue()) {

                    MessageLike messageLike = new MessageLike();
                    messageLike.setId(record.getId().toHexString());
                    messageLike.setAvatar(userInfo.getLogo());
                    messageLike.setNickname(userInfo.getNickName());
                    messageLike.setCreateDate(new DateTime(record.getCreated()).toString("yyyy-MM-dd HH:mm"));

                    messageLikeList.add(messageLike);
                    break;
                }
            }
        }

        pageResult.setItems(messageLikeList);
        return pageResult;
    }

    public PageResult queryMessageAnnouncementList(Integer page, Integer pageSize) {
        IPage<Announcement> announcementIPage = this.announcementService.queryList(page,  pageSize);

        List<MessageAnnouncement> messageAnnouncementList = new ArrayList<>();
        List<Announcement> records = announcementIPage.getRecords();
        for (Announcement record : records) {
            MessageAnnouncement messageAnnouncement = new MessageAnnouncement();
            messageAnnouncement.setId(record.getId().toString());
            messageAnnouncement.setTitle(record.getTitle());
            messageAnnouncement.setDescription(record.getDescription());
            messageAnnouncement.setCreateDate(new DateTime(record.getCreated()).toString("yyyy-MM-dd HH:mm"));
            messageAnnouncementList.add(messageAnnouncement);
        }

        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPages(0);
        pageResult.setCounts(0);
        pageResult.setPageSize(pageSize);
        pageResult.setItems(messageAnnouncementList);

        return pageResult;
    }
}

