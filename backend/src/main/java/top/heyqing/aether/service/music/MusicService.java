package top.heyqing.aether.service.music;

import java.util.List;

import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.model.vo.MusicAlbumDetailVO;
import top.heyqing.aether.model.vo.MusicAlbumListVO;
import top.heyqing.aether.model.vo.MusicDetailVO;
import top.heyqing.aether.model.vo.MusicListVO;

/**
 * 音乐公开服务（BackEnd-Plan §5.2 音乐 music 公开）
 *
 * <p>合集列表按自定义/固定类型过滤；单曲搜索独立于文章搜索（§5.2 仅音乐可搜索）；
 * 全部媒体地址走签名 URL（§4.4）。</p>
 */
public interface MusicService {

    /**
     * 合集分页列表（type=1 自定义/2 固定，可空=全部）
     */
    PageResult<MusicAlbumListVO> pageAlbums(Integer type, int page, int size);

    /**
     * 合集详情（含曲目列表，id 升序）
     */
    MusicAlbumDetailVO albumDetail(Long id);

    /**
     * 单曲搜索（keyword 匹配歌名/歌手；albumId 限定合集，可空）
     */
    PageResult<MusicListVO> search(String keyword, Long albumId, int page, int size);

    /**
     * 单曲详情（含歌词、时长、EffectConfig 原始 JSON）
     */
    MusicDetailVO detail(Long id);

    /**
     * 推荐合集（主页推荐位，limit 3-4）
     */
    List<MusicAlbumListVO> recommend(int limit);
}
