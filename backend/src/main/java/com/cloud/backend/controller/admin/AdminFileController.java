package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Result;
import com.cloud.backend.entity.File;
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
    public Result<List<File>> listFiles() {
        return Result.success(fileService.findAll());
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteFile(@PathVariable Long id) {
        fileService.adminDeleteFile(id);
        return Result.success();
    }
}
