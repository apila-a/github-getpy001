package com.tanhua.server.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 点赞列表的vo返回对象
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageLike {

    private String id;      //编号
    private String avatar;      //头像
    private String nickname;       //昵称
    private String createDate;      //点赞时间

}

