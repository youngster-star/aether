package top.heyqing.aether.service.storage;

/**
 * 业务-文件引用管理（BackEnd-Plan §8.3 删除/修改一致性核心）
 *
 * <p>业务表引用 storage_file 时经本服务登记引用；业务删除/换文件时解绑。
 * 引用归零的文件：事务内 status=0 标记（签名访问即刻失效）→
 * 事务提交后异步物理删除 → 失败进补偿 job 每 10 分钟重试（最多 3 天）。</p>
 */
public interface StorageRefService {

    /** 图集图片业务域 */
    String BIZ_ALBUM = "album";

    /** 视频业务域 */
    String BIZ_VIDEO = "video";

    /** 音乐业务域（合集封面与单曲音频/封面共用，与 seed 数据同构） */
    String BIZ_MUSIC = "music";

    /** 书籍业务域（封面 + txt 源文件） */
    String BIZ_BOOK = "book";

    /**
     * 登记引用（幂等：同一业务记录重复引用同一文件时 refCount 递增）
     * <p>需在业务保存事务内调用。</p>
     *
     * @param fileId  文件 ID
     * @param bizType 业务域（album/video/...）
     * @param bizId   业务记录 ID
     */
    void bind(Long fileId, String bizType, Long bizId);

    /**
     * 解绑单文件引用（业务更新换文件时对旧文件调用）
     *
     * @param fileId  旧文件 ID
     * @param bizType 业务域
     * @param bizId   业务记录 ID
     */
    void unbindFile(Long fileId, String bizType, Long bizId);

    /**
     * 解绑业务记录的全部引用（业务删除时调用；引用归零文件自动标记待清理）
     *
     * @param bizType 业务域
     * @param bizId   业务记录 ID
     */
    void unbindBiz(String bizType, Long bizId);
}
