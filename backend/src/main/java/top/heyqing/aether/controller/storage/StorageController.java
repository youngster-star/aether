package top.heyqing.aether.controller.storage;

import java.io.InputStream;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import top.heyqing.aether.aspect.OperationLog;
import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.common.Result;
import top.heyqing.aether.config.StorageProperties;
import top.heyqing.aether.exception.BusinessException;
import top.heyqing.aether.model.dto.StorageInitRequest;
import top.heyqing.aether.model.entity.StorageFile;
import top.heyqing.aether.model.vo.StorageInitVO;
import top.heyqing.aether.model.vo.StorageMergeVO;
import top.heyqing.aether.repository.StorageFileRepository;
import top.heyqing.aether.service.storage.FileUploadService;
import top.heyqing.aether.storage.FileTypeValidator;
import top.heyqing.aether.storage.StorageResource;
import top.heyqing.aether.util.DigestUtil;

/**
 * 文件存储接口（BackEnd-Plan §5.2 存储 storage）
 *
 * <p>init/chunk/merge 需登录（见 SecurityConfig）；file/{fileId} 公开但必须携带
 * 有效签名（HMAC-SHA256(fileId:expires)，防直链下载，见 §4.4）。
 * 媒体流式输出由 Spring ResourceHttpMessageConverter 自动支持 Range（视频拖动依赖）。</p>
 */
@RestController
@RequestMapping("/v1/storage")
public class StorageController {

    private final FileUploadService fileUploadService;
    private final StorageFileRepository storageFileRepository;
    private final StorageProperties storageProperties;

    public StorageController(FileUploadService fileUploadService, StorageFileRepository storageFileRepository,
                             StorageProperties storageProperties) {
        this.fileUploadService = fileUploadService;
        this.storageFileRepository = storageFileRepository;
        this.storageProperties = storageProperties;
    }

    /**
     * 初始化上传：秒传命中直接返回 fileId，否则返回上传会话与断点信息
     */
    @PostMapping("/init")
    @OperationLog(module = "存储", action = "上传")
    public Result<StorageInitVO> init(@Valid @RequestBody StorageInitRequest request) {
        return Result.ok(fileUploadService.init(request));
    }

    /**
     * 上传分片（multipart：uploadId、index 参数 + file 文件体）
     */
    @PostMapping("/chunk")
    public Result<Void> chunk(@RequestParam String uploadId, @RequestParam int index,
                              @RequestPart("file") MultipartFile file) {
        fileUploadService.chunk(uploadId, index, file);
        return Result.ok();
    }

    /**
     * 合并分片：整体 SHA-256 校验后落库，返回 fileId 与签名访问 URL
     */
    @PostMapping("/merge")
    public Result<StorageMergeVO> merge(@RequestParam String uploadId) {
        return Result.ok(fileUploadService.merge(uploadId));
    }

    /**
     * 签名媒体访问（公开 + 签名，防直链下载；支持 Range 流式播放）
     *
     * @param fileId  文件 ID
     * @param expires 签名过期时间戳（秒）
     * @param sign    HMAC-SHA256(fileId:expires)
     */
    @GetMapping("/file/{fileId}")
    public ResponseEntity<Resource> file(@PathVariable Long fileId,
                                         @RequestParam long expires, @RequestParam String sign) {
        // 签名校验：恒时比较防时序攻击；过期/伪造统一 20001
        String expected = DigestUtil.hmacSha256(fileId + ":" + expires, storageProperties.getSignSecret());
        if (!DigestUtil.constantTimeEquals(expected, sign) || expires * 1000 < System.currentTimeMillis()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "签名无效或已过期");
        }
        StorageFile file = storageFileRepository.findById(fileId)
                .filter(f -> f.getStatus() == 1)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        InputStream in = fileUploadService.openFile(file);
        // StorageResource 提供 contentLength：ResourceHttpMessageConverter 据此支持 Range（206 分段响应）
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(FileTypeValidator.mimeTypeOf(file.getExt())))
                .body(new StorageResource(in, file.getSize()));
    }
}
