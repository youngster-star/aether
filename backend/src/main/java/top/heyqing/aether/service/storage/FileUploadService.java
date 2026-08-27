package top.heyqing.aether.service.storage;

import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

import top.heyqing.aether.model.dto.StorageInitRequest;
import top.heyqing.aether.model.vo.StorageInitVO;
import top.heyqing.aether.model.vo.StorageMergeVO;

/**
 * 文件上传业务编排（BackEnd-Plan §8.2：分片上传 / 断点续传 / 秒传）
 *
 * <p>底层存储由 StorageRouter 按 storageType 路由（本地/OSS），
 * 本服务负责：秒传判断、会话管理、分片完整性、合并后 SHA-256 校验、
 * 魔数校验、元数据探测与 storage_file 落库。</p>
 */
public interface FileUploadService {

    /**
     * 初始化上传：秒传命中直接返回 fileId；否则创建上传会话并返回断点信息
     */
    StorageInitVO init(StorageInitRequest request);

    /**
     * 上传单个分片（幂等：重复分片覆盖写入）
     *
     * @param uploadId 上传会话 ID
     * @param index    分片序号（从 0）
     * @param file     分片内容
     */
    void chunk(String uploadId, int index, MultipartFile file);

    /**
     * 合并全部分片：整体 SHA-256 校验 → 魔数校验 → 元数据探测 → 落库
     *
     * @param uploadId 上传会话 ID
     * @return fileId + 签名访问 URL
     */
    StorageMergeVO merge(String uploadId);

    /**
     * 查询已发布文件元数据（签名媒体访问用；不存在或已删除返回 10002）
     */
    top.heyqing.aether.model.entity.StorageFile findPublishedFile(Long fileId);

    /**
     * 打开文件读取流（签名媒体访问用，按 storage_file.storageType 路由）
     */
    InputStream openFile(top.heyqing.aether.model.entity.StorageFile file);

    /**
     * 构建签名访问 URL（HMAC-SHA256(fileId:expires)，有效时长取配置 sign-expire）
     *
     * <p>业务 VO 输出封面等媒体地址时统一走本方法，禁止直接暴露存储路径（BackEnd-Plan §4.4）。</p>
     *
     * @param fileId 文件 ID
     * @return 完整签名 URL（含 context-path，可直接被前端消费）
     */
    String signUrl(Long fileId);
}
