package com.cloud.backend.controller;

import com.cloud.backend.annotation.Log;
import com.cloud.backend.authorization.AuthorizationPolicy;
import com.cloud.backend.constant.FileConstants;
import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.file.AudioPlayResponse;
import com.cloud.backend.dto.file.BatchDownloadRequest;
import com.cloud.backend.dto.file.BatchDownloadResponse;
import com.cloud.backend.dto.file.DirectoryCreateRequest;
import com.cloud.backend.dto.file.FileCopyRequest;
import com.cloud.backend.dto.file.FileMoveRequest;
import com.cloud.backend.dto.file.FileNodeResponse;
import com.cloud.backend.dto.file.FilePreviewResponse;
import com.cloud.backend.dto.file.FileRenameRequest;
import com.cloud.backend.dto.file.FileTreeResponse;
import com.cloud.backend.dto.file.RecycleBinResponse;
import com.cloud.backend.dto.file.SecUploadResponse;
import com.cloud.backend.dto.file.UploadInitRequest;
import com.cloud.backend.dto.file.UploadInitResponse;
import com.cloud.backend.dto.file.UploadMergeRequest;
import com.cloud.backend.dto.file.UploadPolicyResponse;
import com.cloud.backend.dto.file.UploadProgressResponse;
import com.cloud.backend.dto.file.UploadSecRequest;
import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.TargetType;
import com.cloud.backend.entity.File;
import com.cloud.backend.entity.RecycleBin;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.service.file.DownloadService;
import com.cloud.backend.service.file.FileService;
import com.cloud.backend.service.file.PreviewService;
import com.cloud.backend.service.file.RecycleBinService;
import com.cloud.backend.service.file.SearchService;
import com.cloud.backend.service.file.StorageService;
import com.cloud.backend.service.file.UploadService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件管理控制器（用户端，file-module.md 第二节接口清单）。
 * 所有操作基于当前登录用户（AuthorizationPolicy.getCurrentUserId()），
 * 越权访问返回 FILE_NOT_FOUND（不泄露他人文件信息）。
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final UploadService uploadService;
    private final DownloadService downloadService;
    private final PreviewService previewService;
    private final SearchService searchService;
    private final RecycleBinService recycleBinService;
    private final StorageService storageService;

    public FileController(FileService fileService, UploadService uploadService, DownloadService downloadService,
                          PreviewService previewService, SearchService searchService,
                          RecycleBinService recycleBinService, StorageService storageService) {
        this.fileService = fileService;
        this.uploadService = uploadService;
        this.downloadService = downloadService;
        this.previewService = previewService;
        this.searchService = searchService;
        this.recycleBinService = recycleBinService;
        this.storageService = storageService;
    }

    /* ==================== 列表 / 树 / 目录 ==================== */

    @GetMapping
    public Result<Page<FileNodeResponse>> list(@RequestParam(required = false) Long parentId,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return Result.success(fileService.pageByUserAndParent(
                AuthorizationPolicy.getCurrentUserId(), parentId == null ? 0L : parentId, page, size));
    }

    @GetMapping("/tree")
    public Result<List<FileTreeResponse>> tree() {
        return Result.success(fileService.tree(AuthorizationPolicy.getCurrentUserId()));
    }

    @PostMapping("/directory")
    public Result<FileNodeResponse> createDirectory(@Valid @RequestBody DirectoryCreateRequest request) {
        return Result.success(FileNodeResponse.from(
                fileService.createDirectory(AuthorizationPolicy.getCurrentUserId(), request)));
    }

    /* ==================== 上传（init / chunk / merge / sec / progress） ==================== */

    @PostMapping("/upload/init")
    public Result<UploadInitResponse> uploadInit(@Valid @RequestBody UploadInitRequest request) {
        return Result.success(uploadService.init(AuthorizationPolicy.getCurrentUserId(), request));
    }

    @GetMapping("/upload/policy")
    public Result<UploadPolicyResponse> uploadPolicy() {
        return Result.success(uploadService.policy(AuthorizationPolicy.getCurrentUserId()));
    }

    @PostMapping("/upload/chunk")
    public Result<Void> uploadChunk(@RequestParam String uploadId,
                                    @RequestParam int seq,
                                    @RequestParam("file") MultipartFile file) {
        uploadService.uploadChunk(AuthorizationPolicy.getCurrentUserId(), uploadId, seq, file);
        return Result.success();
    }

    @PostMapping("/upload/merge")
    public Result<FileNodeResponse> uploadMerge(@Valid @RequestBody UploadMergeRequest request) {
        return Result.success(uploadService.merge(AuthorizationPolicy.getCurrentUserId(), request.getUploadId()));
    }

    @PostMapping("/upload/sec")
    public Result<SecUploadResponse> uploadSec(@Valid @RequestBody UploadSecRequest request) {
        return Result.success(uploadService.sec(AuthorizationPolicy.getCurrentUserId(), request));
    }

    @GetMapping("/upload/progress/{uploadId}")
    public Result<UploadProgressResponse> uploadProgress(@PathVariable String uploadId) {
        return Result.success(uploadService.progress(AuthorizationPolicy.getCurrentUserId(), uploadId));
    }

    /* ==================== 下载 ==================== */

    @GetMapping("/{id}/download")
    @Log(operation = OperationType.DOWNLOAD_FILE, target = TargetType.FILE, targetId = "#id",
         detail = "'下载文件'")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        String url = downloadService.getDownloadUrl(AuthorizationPolicy.getCurrentUserId(), id);
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", url);
    }

    @PostMapping("/download/batch")
    public Result<BatchDownloadResponse> downloadBatch(@Valid @RequestBody BatchDownloadRequest request) {
        return Result.success(downloadService.createBatchTask(
                AuthorizationPolicy.getCurrentUserId(), request.getFileIds()));
    }

    @GetMapping("/download/batch/{taskId}")
    public Result<BatchDownloadResponse> downloadBatchStatus(@PathVariable String taskId) {
        return Result.success(downloadService.getBatchTask(taskId));
    }

    /* ==================== 重命名 / 移动 / 复制 / 删除 ==================== */

    @PutMapping("/{id}/rename")
    public Result<FileNodeResponse> rename(@PathVariable Long id,
                                           @Valid @RequestBody FileRenameRequest request) {
        return Result.success(fileService.rename(AuthorizationPolicy.getCurrentUserId(), id, request.getName()));
    }

    @PostMapping("/{id}/move")
    public Result<FileNodeResponse> move(@PathVariable Long id,
                                         @Valid @RequestBody FileMoveRequest request) {
        return Result.success(fileService.move(
                AuthorizationPolicy.getCurrentUserId(), id, request.getTargetParentId()));
    }

    @PostMapping("/{id}/copy")
    public Result<FileNodeResponse> copy(@PathVariable Long id,
                                         @Valid @RequestBody FileCopyRequest request) {
        return Result.success(fileService.copy(
                AuthorizationPolicy.getCurrentUserId(), id, request.getTargetParentId()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.deleteToRecycle(AuthorizationPolicy.getCurrentUserId(), id);
        return Result.success();
    }

    /* ==================== 搜索 / 预览 ==================== */

    @GetMapping("/search")
    public Result<Page<FileNodeResponse>> search(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer category,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return Result.success(searchService.search(
                AuthorizationPolicy.getCurrentUserId(), keyword, category, page, size));
    }

    @GetMapping("/{id}/preview")
    public Result<FilePreviewResponse> preview(@PathVariable Long id) {
        return Result.success(previewService.preview(AuthorizationPolicy.getCurrentUserId(), id));
    }

    /* ==================== 音乐播放器预留接口（file-module.md 十二.10，本期无 UI） ==================== */

    /** 音频列表（分页，个人空间 category=AUDIO） */
    @GetMapping("/audio/list")
    public Result<Page<FileNodeResponse>> audioList(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return Result.success(fileService.listAudio(AuthorizationPolicy.getCurrentUserId(), page, size));
    }

    /** 播放地址（校验归属 + 音频类型，返回 presigned URL 直连 MinIO） */
    @GetMapping("/{id}/play")
    public Result<AudioPlayResponse> play(@PathVariable Long id) {
        File file = fileService.getOwnedFile(AuthorizationPolicy.getCurrentUserId(), id);
        if (file.isDir() || file.getObjectName() == null || file.getObjectName().isEmpty()
                || file.getCategory() == null || file.getCategory() != FileConstants.AUDIO) {
            throw new BusinessException(ErrorCode.PREVIEW_UNSUPPORTED);
        }
        AudioPlayResponse response = new AudioPlayResponse();
        response.setFileId(file.getId());
        response.setName(file.getName());
        response.setUrl(storageService.generateDownloadUrl(file.getObjectName(), 10));
        return Result.success(response);
    }

    /* ==================== 回收站 ==================== */

    @GetMapping("/recycle-bin")
    public Result<List<RecycleBinResponse>> recycleBin() {
        List<RecycleBinResponse> records = recycleBinService
                .listByUserId(AuthorizationPolicy.getCurrentUserId()).stream()
                .map(RecycleBinResponse::from)
                .toList();
        return Result.success(records);
    }

    @PostMapping("/recycle-bin/{id}/restore")
    public Result<Void> restore(@PathVariable Long id) {
        recycleBinService.restore(AuthorizationPolicy.getCurrentUserId(), id);
        return Result.success();
    }

    @DeleteMapping("/recycle-bin/{id}")
    public Result<Void> purgeRecycle(@PathVariable Long id) {
        recycleBinService.purge(AuthorizationPolicy.getCurrentUserId(), id);
        return Result.success();
    }
}
