package com.tanhua.dubbo.server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.ArrayList;

@Data
@Document(collection = "recommend_user")
@AllArgsConstructor
@NoArgsConstructor
public class RecommendUser implements Serializable {
    private static final long serialVersionUID = 8683452581122892189L;

    @Id
    private ObjectId id;
    @Indexed
    private Long userId;
    @Indexed
    private Double score;
    private Long toUserId;
    private String date;

}
