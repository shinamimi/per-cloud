package com.cloud.backend.dto.file;

import com.cloud.backend.entity.RecycleBin;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 回收站记录响应。
 */
@Data
public class RecycleBinResponse {

    private Long id;
    private Long fileId;
    private String originalName;
    private Integer type;
    private Long size;
    private LocalDateTime deletedTime;
    private LocalDateTime expireTime;

    public static RecycleBinResponse from(RecycleBin recycleBin) {
        RecycleBinResponse response = new RecycleBinResponse();
        response.setId(recycleBin.getId());
        response.setFileId(recycleBin.getFileId());
        response.setOriginalName(recycleBin.getOriginalName());
        response.setType(recycleBin.getType());
        response.setSize(recycleBin.getSize());
        response.setDeletedTime(recycleBin.getDeletedTime());
        response.setExpireTime(recycleBin.getExpireTime());
        return response;
    }
}
