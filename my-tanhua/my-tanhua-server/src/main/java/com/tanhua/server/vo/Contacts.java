package com.tanhua.server.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *联系人列表
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contacts {

    private Long id;
    private String userId;   //用户id
    private String avatar;      //头像
    private String nickname;    //昵称
    private String gender;      //性别
    private Integer age;        //年龄
    private String city;        //城市

}

