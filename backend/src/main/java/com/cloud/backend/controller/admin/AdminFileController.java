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

/**
 * 管理端全局文件管控（仅 ADMIN+）。
 * 覆盖个人文件 + 团队文件：列表筛选/详情/禁用启用/删除（进全局回收站）/全局回收站管理。
 *
 * 修改指引：
 * - 【习惯】全局文件列表       → GET /api/admin/files（AdminFileQuery 绑定查询参数），调 adminFileService.page；
 *                        路径权限在 SecurityConfig（/api/admin/files/** hasAnyRole("ADMIN","SUPER_ADMIN")）
 * - 【习惯】文件详情 / 下载 / 预览 → GET /api/admin/files/{id}、/{id}/download（302 预签名重定向）、/{id}/preview（不受禁用限制）；
 *                        调 detail / detailEntity / generateDownloadUrl / previewFileForAdmin
 * - 【习惯】禁用 / 启用        → PUT /api/admin/files/{id}/status（scope：GLOBAL 全站禁 / USER 仅用户，默认 USER）、
 *                        POST /api/admin/files/batch-status；调 changeStatus
 * - 【习惯】删除（进全局回收站） → DELETE /api/admin/files/{id} 或 DELETE /api/admin/files（body=ids 批量）；
 *                        调 deleteToGlobalRecycleBin
 * - 【习惯】全局回收站         → GET /api/admin/files/recycle-bin、PUT /recycle-bin/{id}/restore、
 *                        DELETE /recycle-bin（body=ids 批量）或 /recycle-bin/{id}；调 globalRecycleBin / restore / purge
 * - 【习惯】新增/修改接口       → 接口权限在 SecurityConfig（/api/admin/files/** 仅 ADMIN/SUPER_ADMIN），
 *                        如需降低放行级别需改此处并评估文件管控风险；前端管理端 API 层需同步
 * - 【习惯】查询参数           → AdminFileQuery 绑定 GET 查询参数（userId/teamId/category/status/sort/page/size），
 *                        改动需同步前端筛选与分页组件
 */
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
