package com.cloud.backend.controller.admin;

import com.cloud.backend.constant.FileConstants;
import com.cloud.backend.dto.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {

    @GetMapping
    public Result<Map<String, Object>> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("defaultQuota", FileConstants.DEFAULT_QUOTA);
        settings.put("defaultChunkSize", FileConstants.DEFAULT_CHUNK_SIZE);
        return Result.success(settings);
    }
}
