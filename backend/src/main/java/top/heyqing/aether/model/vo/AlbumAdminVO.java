package top.heyqing.aether.model.vo;

import java.time.LocalDateTime;

/**
 * 图集管理 VO（BackEnd-Plan §5.2 /admin/albums，比公开 VO 多推荐位与文件 ID）
 *
 * @param id          图集 ID
 * @param title       标题
 * @param intro       介绍
 * @param coverFileId 封面文件 ID（可空）
 * @param coverUrl    封面签名 URL（可空）
 * @param isRecommend 是否推荐
 * @param sort        推荐排序号
 * @param imageCount  图片数量
 * @param createTime  创建时间
 * @param updateTime  更新时间
 */
public record AlbumAdminVO(Long id, String title, String intro, Long coverFileId, String coverUrl,
                           Integer isRecommend, Integer sort, long imageCount,
                           LocalDateTime createTime, LocalDateTime updateTime) {
}
