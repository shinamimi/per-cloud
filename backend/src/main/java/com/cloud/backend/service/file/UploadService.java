package com.cloud.backend.service.file;

import com.cloud.backend.dto.file.FileNodeResponse;
import com.cloud.backend.dto.file.SecUploadResponse;
import com.cloud.backend.dto.file.UploadInitRequest;
import com.cloud.backend.dto.file.UploadInitResponse;
import com.cloud.backend.dto.file.UploadPolicyResponse;
import com.cloud.backend.dto.file.UploadProgressResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {

    UploadInitResponse init(Long userId, UploadInitRequest request);

    UploadPolicyResponse policy(Long userId);

    void uploadChunk(Long userId, String uploadId, int seq, MultipartFile file);

    UploadProgressResponse progress(Long userId, String uploadId);

    FileNodeResponse merge(Long userId, String uploadId);

    SecUploadResponse sec(Long userId, com.cloud.backend.dto.file.UploadSecRequest request);
}
