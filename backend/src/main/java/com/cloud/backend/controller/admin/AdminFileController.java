package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.admin.AdminFileResponse;
import com.cloud.backend.service.file.FileService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/files")
public class AdminFileController {

    private final FileService fileService;

    public AdminFileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    public Result<List<AdminFileResponse>> listFiles() {
        List<AdminFileResponse> files = fileService.findAll().stream()
                .map(f -> new AdminFileResponse(f.getId(), f.getUserId(), f.getParentId(),
                        f.getName(), f.getPath(), f.getSize(), f.getMimeType(), f.getExtension(),
                        f.getIsDirectory(), f.getStatus(), f.getCreatedAt(), f.getUpdatedAt()))
                .toList();
        return Result.success(files);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteFile(@PathVariable Long id) {
        fileService.adminDeleteFile(id);
        return Result.success();
    }
}
