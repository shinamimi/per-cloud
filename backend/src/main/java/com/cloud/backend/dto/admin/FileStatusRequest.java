package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.DisableScope;
import com.cloud.backend.enums.FileStatus;
import lombok.Data;

@Data
public class FileStatusRequest {

    private FileStatus status;
    private DisableScope scope = DisableScope.USER;
}
