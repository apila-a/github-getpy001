package com.tanhua.dubbo.server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageInfo<T> implements Serializable {
    private static final long serialVersionUID = 8683452581122892189L;

    /**
     * 总条数
     */
    private Integer total=0;

    /**
     * 当前页
     */
    private Integer pageNum=0;
    /**
     * 一页的显示条数
     */
    private Integer pageSize=0;
    /**
     * 数据列表
     */
    private List<T> records= Collections.emptyList();


}
