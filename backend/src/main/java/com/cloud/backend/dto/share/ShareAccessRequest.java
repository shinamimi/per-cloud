package com.cloud.backend.dto.share;

import lombok.Data;

import java.util.List;

@Data
public class ShareAccessRequest {

    private List<Long> snapshotIds;
}
