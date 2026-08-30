package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.DisableScope;
import com.cloud.backend.enums.FileStatus;
import lombok.Data;

import java.util.List;

@Data
public class BatchFileStatusRequest {

    private List<Long> ids;
    private FileStatus status;
    private DisableScope scope = DisableScope.USER;
}
