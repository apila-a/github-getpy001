package com.tanhua.dubbo.server.api;

import com.tanhua.dubbo.server.pojo.PageInfo;
import com.tanhua.dubbo.server.pojo.Publish;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class testQuanzi {

    @Autowired
    private QuanZiApi quanZiApi;

    @Test
    public void testSavePublish(){
        Publish publish = new Publish();
        publish.setUserId(1L);
        publish.setLocationName("上海市");
        publish.setSeeType(1);
        publish.setText("今天天气不错~");
        publish.setMedias(Arrays.asList("https://itcast-tanhua.oss-cn-shanghai.aliyuncs.com/images/quanzi/1.jpg"));
        String result = this.quanZiApi.savePublish(publish);
        System.out.println(result);
    }

    @Test
    public void testQueryPublish(){
        PageInfo<Publish> publishPageInfo = quanZiApi.queryPublishList(1l, 1, 10);
        System.out.println("123333  "+publishPageInfo.getRecords());
    }
}
