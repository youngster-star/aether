package top.heyqing.aether.service.album;

import java.util.List;

import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.model.vo.AlbumDetailVO;
import top.heyqing.aether.model.vo.AlbumListVO;

/**
 * 图集公开服务（BackEnd-Plan §5.2 图集 album 公开）
 */
public interface AlbumService {

    /**
     * 图集分页列表（keyword 搜索标题/介绍）
     */
    PageResult<AlbumListVO> pageList(int page, int size, String keyword);

    /**
     * 推荐图集（is_recommend=1，推荐排序号大者优先）
     */
    List<AlbumListVO> recommend(int limit);

    /**
     * 图集详情（含图片列表：签名 URL、标题、介绍、宽高、大小）
     */
    AlbumDetailVO detail(Long id);
}
