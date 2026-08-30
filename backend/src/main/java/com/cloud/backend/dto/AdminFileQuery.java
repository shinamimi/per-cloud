package com.cloud.backend.dto;

import lombok.Data;

@Data
public class AdminFileQuery {

    private Long userId;
    private String username;
    private Long teamId;
    private Integer category;
    private Integer status;
    private String sort;
    private int page = 1;
    private int size = 20;
    /** 分页偏移（由 service 根据 page/size 计算，XML 分页用） */
    private int offset = 0;
}
