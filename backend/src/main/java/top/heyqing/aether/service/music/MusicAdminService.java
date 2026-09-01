package top.heyqing.aether.service.music;

import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.model.dto.MusicAlbumSaveRequest;
import top.heyqing.aether.model.dto.MusicSaveRequest;
import top.heyqing.aether.model.vo.MusicAdminVO;
import top.heyqing.aether.model.vo.MusicAlbumAdminVO;

/**
 * 音乐管理服务（BackEnd-Plan §5.2 /admin/music/albums + /admin/music）
 *
 * <p>音频/封面文件引用经 StorageRefService 登记（bizType=music，与 seed 同构）；
 * 曲目删除/换文件走引用归零清理链路（§8.3）；特效生成结果落库前必须过
 * EffectConfig Schema 校验（§7.3 防幻觉字段）。</p>
 */
public interface MusicAdminService {

    /**
     * 合集管理分页列表
     */
    PageResult<MusicAlbumAdminVO> albumList(int page, int size, String keyword);

    /**
     * 新建合集（type=2 固定合集要求认证信息）
     */
    Long albumCreate(MusicAlbumSaveRequest request);

    /**
     * 编辑合集（换封面走旧引用解绑 + 新引用登记）
     */
    void albumUpdate(Long id, MusicAlbumSaveRequest request);

    /**
     * 删除合集（含曲目时拒绝，需先移除曲目；封面引用归零清理）
     */
    void albumDelete(Long id);

    /**
     * 单曲管理分页列表（keyword 匹配歌名/歌手；albumId 限定）
     */
    PageResult<MusicAdminVO> list(int page, int size, String keyword, Long albumId);

    /**
     * 新建单曲（时长默认取上传探测值，探测失败可手工补录）
     */
    Long create(MusicSaveRequest request);

    /**
     * 编辑单曲（换文件/封面走旧引用解绑；effectConfig 提交时过 Schema 校验）
     */
    void update(Long id, MusicSaveRequest request);

    /**
     * 删除单曲（音频/封面引用归零清理）
     */
    void delete(Long id);

    /**
     * 生成播放特效（EffectConfig，§7.3）：调色板提取 + 规则生成，
     * 校验通过后落库 effect_config 并置 effect_source=1
     *
     * @return 生成的配置 JSON（管理端预览回显）
     */
    String generateEffect(Long id);
}
