package com.cloud.backend.controller;

import com.cloud.backend.annotation.Log;
import com.cloud.backend.annotation.RateLimit;
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
    private final com.cloud.backend.service.admin.AdminSettingsService adminSettingsService;

    public FileController(FileService fileService, UploadService uploadService, DownloadService downloadService,
                          PreviewService previewService, SearchService searchService,
                          RecycleBinService recycleBinService, StorageService storageService,
                          com.cloud.backend.service.admin.AdminSettingsService adminSettingsService) {
        this.fileService = fileService;
        this.uploadService = uploadService;
        this.downloadService = downloadService;
        this.previewService = previewService;
        this.searchService = searchService;
        this.recycleBinService = recycleBinService;
        this.storageService = storageService;
        this.adminSettingsService = adminSettingsService;
    }

    /* ==================== 列表 / 树 / 目录 ==================== */

    /**
     * 分页查询当前用户指定目录下的文件列表，parentId 为空时查询根目录。
     */
    @GetMapping
    public Result<Page<FileNodeResponse>> list(@RequestParam(required = false) Long parentId,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return Result.success(fileService.pageByUserAndParent(
                AuthorizationPolicy.getCurrentUserId(), parentId == null ? 0L : parentId, page, size));
    }

    /**
     * 获取当前用户的完整目录树（用于导航/移动文件时选择目标目录）。
     */
    @GetMapping("/tree")
    public Result<List<FileTreeResponse>> tree() {
        return Result.success(fileService.tree(AuthorizationPolicy.getCurrentUserId()));
    }

    /**
     * 在当前用户空间创建目录，目录名校验与重名校验由服务层负责。
     */
    @PostMapping("/directory")
    public Result<FileNodeResponse> createDirectory(@Valid @RequestBody DirectoryCreateRequest request) {
        return Result.success(FileNodeResponse.from(
                fileService.createDirectory(AuthorizationPolicy.getCurrentUserId(), request)));
    }

    /* ==================== 上传（init / chunk / merge / sec / progress） ==================== */

    /**
     * 初始化分片上传，返回 uploadId 与各分片的预签名上传地址。
     */
    @PostMapping("/upload/init")
    @RateLimit(key = "upload", limit = 100, window = 60, dimension = RateLimit.Dimension.USER)
    public Result<UploadInitResponse> uploadInit(@Valid @RequestBody UploadInitRequest request) {
        return Result.success(uploadService.init(AuthorizationPolicy.getCurrentUserId(), request));
    }

    /**
     * 查询当前用户的有效上传策略（预签名有效期等），用于前端初始化上传。
     */
    @GetMapping("/upload/policy")
    public Result<UploadPolicyResponse> uploadPolicy() {
        return Result.success(uploadService.policy(AuthorizationPolicy.getCurrentUserId()));
    }

    /**
     * 上传单个分片（按 uploadId + 分片序号定位），文件内容由请求体携带。
     */
    @PostMapping("/upload/chunk")
    public Result<Void> uploadChunk(@RequestParam String uploadId,
                                    @RequestParam int seq,
                                    @RequestParam("file") MultipartFile file) {
        uploadService.uploadChunk(AuthorizationPolicy.getCurrentUserId(), uploadId, seq, file);
        return Result.success();
    }

    /**
     * 合并已上传的分片，完成文件落库并返回文件节点信息。
     */
    @PostMapping("/upload/merge")
    public Result<FileNodeResponse> uploadMerge(@Valid @RequestBody UploadMergeRequest request) {
        return Result.success(uploadService.merge(AuthorizationPolicy.getCurrentUserId(), request.getUploadId()));
    }

    /**
     * 秒传：按文件哈希快速复用已有文件，命中则跳过实际上传。
     */
    @PostMapping("/upload/sec")
    public Result<SecUploadResponse> uploadSec(@Valid @RequestBody UploadSecRequest request) {
        return Result.success(uploadService.sec(AuthorizationPolicy.getCurrentUserId(), request));
    }

    /**
     * 查询指定上传任务的分片进度，用于前端断点续传与进度展示。
     */
    @GetMapping("/upload/progress/{uploadId}")
    public Result<UploadProgressResponse> uploadProgress(@PathVariable String uploadId) {
        return Result.success(uploadService.progress(AuthorizationPolicy.getCurrentUserId(), uploadId));
    }

    /* ==================== 下载 ==================== */

    /**
     * 下载文件：302 重定向到预签名下载地址，并记录下载操作日志。
     */
    @GetMapping("/{id}/download")
    @Log(operation = OperationType.DOWNLOAD_FILE, target = TargetType.FILE, targetId = "#id",
         detail = "'下载文件'")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        String url = downloadService.getDownloadUrl(AuthorizationPolicy.getCurrentUserId(), id);
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", url);
    }

    /**
     * 创建批量下载任务（打包为压缩文件），返回任务 ID 供轮询进度。
     */
    @PostMapping("/download/batch")
    @RateLimit(key = "download", limit = 200, window = 60, dimension = RateLimit.Dimension.USER)
    public Result<BatchDownloadResponse> downloadBatch(@Valid @RequestBody BatchDownloadRequest request) {
        return Result.success(downloadService.createBatchTask(
                AuthorizationPolicy.getCurrentUserId(), request.getFileIds()));
    }

    /**
     * 查询批量下载任务状态与打包产物地址。
     */
    @GetMapping("/download/batch/{taskId}")
    public Result<BatchDownloadResponse> downloadBatchStatus(@PathVariable String taskId) {
        return Result.success(downloadService.getBatchTask(taskId));
    }

    /* ==================== 重命名 / 移动 / 复制 / 删除 ==================== */

    /**
     * 重命名文件或目录（同名校验由服务层负责）。
     */
    @PutMapping("/{id}/rename")
    public Result<FileNodeResponse> rename(@PathVariable Long id,
                                           @Valid @RequestBody FileRenameRequest request) {
        return Result.success(fileService.rename(AuthorizationPolicy.getCurrentUserId(), id, request.getName()));
    }

    /**
     * 移动文件到目标目录（跨目录移动，目标目录必须属于当前用户）。
     */
    @PostMapping("/{id}/move")
    public Result<FileNodeResponse> move(@PathVariable Long id,
                                         @Valid @RequestBody FileMoveRequest request) {
        return Result.success(fileService.move(
                AuthorizationPolicy.getCurrentUserId(), id, request.getTargetParentId()));
    }

    /**
     * 复制文件到目标目录（复制目录时递归复制其子文件）。
     */
    @PostMapping("/{id}/copy")
    public Result<FileNodeResponse> copy(@PathVariable Long id,
                                         @Valid @RequestBody FileCopyRequest request) {
        return Result.success(fileService.copy(
                AuthorizationPolicy.getCurrentUserId(), id, request.getTargetParentId()));
    }

    /**
     * 删除文件（移入回收站，可从回收站恢复）。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.deleteToRecycle(AuthorizationPolicy.getCurrentUserId(), id);
        return Result.success();
    }

    /* ==================== 搜索 / 预览 ==================== */

    /**
     * 按关键字/分类搜索当前用户空间内的文件，分页返回。
     */
    @GetMapping("/search")
    public Result<Page<FileNodeResponse>> search(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer category,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return Result.success(searchService.search(
                AuthorizationPolicy.getCurrentUserId(), keyword, category, page, size));
    }

    /**
     * 获取文件预览信息（文本/图片等支持预览的类型返回相应地址）。
     */
    @GetMapping("/{id}/preview")
    public Result<FilePreviewResponse> preview(@PathVariable Long id) {
        return Result.success(previewService.preview(AuthorizationPolicy.getCurrentUserId(), id));
    }

    /* ==================== 音乐播放器预留接口（本期无 UI，仅保留能力） ==================== */

    /**
     * 音频列表（分页，个人空间 category=AUDIO）
     */
    @GetMapping("/audio/list")
    public Result<Page<FileNodeResponse>> audioList(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return Result.success(fileService.listAudio(AuthorizationPolicy.getCurrentUserId(), page, size));
    }

    /**
     * 播放地址（校验归属 + 音频类型，返回 presigned URL 直连 MinIO）
     */
    @GetMapping("/{id}/play")
    public Result<AudioPlayResponse> play(@PathVariable Long id) {
        File file = fileService.getOwnedFile(AuthorizationPolicy.getCurrentUserId(), id);
        // 仅音频文件提供播放地址：目录、无存储对象或非音频分类一律拒绝
        if (file.isDir() || file.getObjectName() == null || file.getObjectName().isEmpty()
                || file.getCategory() == null || file.getCategory() != FileConstants.AUDIO) {
            throw new BusinessException(ErrorCode.PREVIEW_UNSUPPORTED);
        }
        AudioPlayResponse response = new AudioPlayResponse();
        response.setFileId(file.getId());
        response.setName(file.getName());
        response.setUrl(storageService.generateDownloadUrl(file.getObjectName(), adminSettingsService.getDownloadLinkTtlMinutes()));
        return Result.success(response);
    }

    /* ==================== 回收站 ==================== */

    /**
     * 查询当前用户的回收站记录列表。
     */
    @GetMapping("/recycle-bin")
    public Result<List<RecycleBinResponse>> recycleBin() {
        List<RecycleBinResponse> records = recycleBinService
                .listByUserId(AuthorizationPolicy.getCurrentUserId()).stream()
                .map(RecycleBinResponse::from)
                .toList();
        return Result.success(records);
    }

    /**
     * 从回收站恢复文件到原目录（原父目录不可用时要求先恢复父目录；同名冲突自动追加后缀）。
     */
    @PostMapping("/recycle-bin/{id}/restore")
    public Result<Void> restore(@PathVariable Long id) {
        recycleBinService.restore(AuthorizationPolicy.getCurrentUserId(), id);
        return Result.success();
    }

    /**
     * 从回收站彻底删除记录，不可恢复。
     */
    @DeleteMapping("/recycle-bin/{id}")
    public Result<Void> purgeRecycle(@PathVariable Long id) {
        recycleBinService.purge(AuthorizationPolicy.getCurrentUserId(), id);
        return Result.success();
    }
}
