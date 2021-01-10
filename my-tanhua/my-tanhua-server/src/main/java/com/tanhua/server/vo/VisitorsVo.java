package com.tanhua.server.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 谁看过我 返回对象
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitorsVo {

    private Long id;
    private String avatar;
    private String nickname;
    private String gender;
    private Integer age;
    private String[] tags;
    private Integer fateValue;

}

