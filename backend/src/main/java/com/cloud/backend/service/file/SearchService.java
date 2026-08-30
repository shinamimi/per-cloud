package com.cloud.backend.service.file;

import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.file.FileNodeResponse;

public interface SearchService {

    Page<FileNodeResponse> search(Long userId, String keyword, Integer category, int page, int size);
}
