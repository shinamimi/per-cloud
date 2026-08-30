package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.AdminFileQuery;
import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.admin.AdminFileResponse;
import com.cloud.backend.dto.admin.AdminRecycleResponse;
import com.cloud.backend.dto.admin.FileStatusRequest;
import com.cloud.backend.dto.file.FilePreviewResponse;
import com.cloud.backend.service.admin.AdminFileService;
import com.cloud.backend.service.file.PreviewService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/files")
public class AdminFileController {

    private final AdminFileService adminFileService;
    private final PreviewService previewService;

    public AdminFileController(AdminFileService adminFileService, PreviewService previewService) {
        this.adminFileService = adminFileService;
        this.previewService = previewService;
    }

    /** 全局文件列表（筛选+分页）—— GET /api/admin/files?userId=&teamId=&category=&status=&sort=&page=&size= */
    @GetMapping
    public Result<Page<AdminFileResponse>> listFiles(AdminFileQuery query) {
        return Result.success(adminFileService.page(query));
    }

    /** 文件详情 —— GET /api/admin/files/{id} */
    @GetMapping("/{id}")
    public Result<AdminFileResponse> fileDetail(@PathVariable Long id) {
        return Result.success(adminFileService.detail(id));
    }

    /** 管理员下载 —— GET /api/admin/files/{id}/download */
    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        com.cloud.backend.entity.File file = adminFileService.detailEntity(id);
        String url = adminFileService.generateDownloadUrl(file);
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", url);
    }

    /** 管理员预览 —— GET /api/admin/files/{id}/preview（不受禁用限制，用于决定解禁） */
    @GetMapping("/{id}/preview")
    public Result<FilePreviewResponse> preview(@PathVariable Long id) {
        com.cloud.backend.entity.File file = adminFileService.detailEntity(id);
        return Result.success(previewService.previewFileForAdmin(file));
    }

    /** 禁用/启用 —— PUT /api/admin/files/{id}/status（scope：GLOBAL=全站禁/USER=仅用户，默认 USER） */
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody FileStatusRequest request) {
        adminFileService.changeStatus(id, request.getStatus(), request.getScope());
        return Result.success();
    }

    /** 批量禁用/启用 —— POST /api/admin/files/batch-status */
    @PostMapping("/batch-status")
    public Result<Void> batchChangeStatus(@RequestBody com.cloud.backend.dto.admin.BatchFileStatusRequest request) {
        for (Long id : request.getIds()) {
            adminFileService.changeStatus(id, request.getStatus(), request.getScope());
        }
        return Result.success();
    }

    /** 删除（进全局回收站）—— DELETE /api/admin/files/{id} 或 DELETE /api/admin/files（body=ids 批量） */
    @DeleteMapping
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        adminFileService.deleteToGlobalRecycleBin(ids);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteFile(@PathVariable Long id) {
        adminFileService.deleteToGlobalRecycleBin(List.of(id));
        return Result.success();
    }

    /** 全局回收站列表 —— GET /api/admin/files/recycle-bin */
    @GetMapping("/recycle-bin")
    public Result<List<AdminRecycleResponse>> recycleBin() {
        return Result.success(adminFileService.globalRecycleBin());
    }

    /** 恢复 —— PUT /api/admin/files/recycle-bin/{id}/restore */
    @PutMapping("/recycle-bin/{id}/restore")
    public Result<Void> restore(@PathVariable Long id) {
        adminFileService.restore(id);
        return Result.success();
    }

    /** 彻底删除（支持批量）—— DELETE /api/admin/files/recycle-bin（body=ids） */
    @DeleteMapping("/recycle-bin")
    public Result<Void> purgeBatch(@RequestBody List<Long> ids) {
        adminFileService.purge(ids);
        return Result.success();
    }

    @DeleteMapping("/recycle-bin/{id}")
    public Result<Void> purge(@PathVariable Long id) {
        adminFileService.purge(List.of(id));
        return Result.success();
    }
}
