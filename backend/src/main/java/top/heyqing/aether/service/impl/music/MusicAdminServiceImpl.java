package top.heyqing.aether.service.impl.music;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.exception.BusinessException;
import top.heyqing.aether.model.dto.MusicAlbumSaveRequest;
import top.heyqing.aether.model.dto.MusicSaveRequest;
import top.heyqing.aether.model.entity.Music;
import top.heyqing.aether.model.entity.MusicAlbum;
import top.heyqing.aether.model.entity.StorageFile;
import top.heyqing.aether.model.vo.MusicAdminVO;
import top.heyqing.aether.model.vo.MusicAlbumAdminVO;
import top.heyqing.aether.repository.MusicAlbumRepository;
import top.heyqing.aether.repository.MusicRepository;
import top.heyqing.aether.repository.StorageFileRepository;
import top.heyqing.aether.service.music.EffectConfigService;
import top.heyqing.aether.service.music.MusicAdminService;
import top.heyqing.aether.service.storage.FileUploadService;
import top.heyqing.aether.service.storage.StorageRefService;
import top.heyqing.aether.storage.FileTypeValidator;
import top.heyqing.aether.util.EffectConfigValidator;

/**
 * 音乐管理实现（BackEnd-Plan §5.2 /admin/music/albums + /admin/music）
 *
 * <p>音频与封面引用统一登记在 bizType=music（与 seed 数据同构）；
 * 合集删除含曲目时拒绝（防误删曲目链路）；特效生成/手工调参落库前
 * 均过 {@link EffectConfigValidator}。</p>
 */
@Service
public class MusicAdminServiceImpl implements MusicAdminService {

    /** 固定合集类型值 */
    private static final int TYPE_CERTIFIED = 2;

    private final MusicRepository musicRepository;
    private final MusicAlbumRepository musicAlbumRepository;
    private final StorageFileRepository storageFileRepository;
    private final FileUploadService fileUploadService;
    private final StorageRefService storageRefService;
    private final EffectConfigService effectConfigService;

    public MusicAdminServiceImpl(MusicRepository musicRepository, MusicAlbumRepository musicAlbumRepository,
                                 StorageFileRepository storageFileRepository, FileUploadService fileUploadService,
                                 StorageRefService storageRefService, EffectConfigService effectConfigService) {
        this.musicRepository = musicRepository;
        this.musicAlbumRepository = musicAlbumRepository;
        this.storageFileRepository = storageFileRepository;
        this.fileUploadService = fileUploadService;
        this.storageRefService = storageRefService;
        this.effectConfigService = effectConfigService;
    }

    // ===== 合集管理 =====

    @Override
    public PageResult<MusicAlbumAdminVO> albumList(int page, int size, String keyword) {
        Specification<MusicAlbum> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(cb.like(root.get("title"), pattern),
                        cb.like(root.get("intro"), pattern)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<MusicAlbum> result = musicAlbumRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));
        List<MusicAlbumAdminVO> records = toAlbumAdminVOs(result.getContent());
        return PageResult.of(records, result.getTotalElements(), page, size);
    }    @Override
    @Transactional
    public Long albumCreate(MusicAlbumSaveRequest request) {
        MusicAlbum album = new MusicAlbum();
        applyAlbumRequest(album, request);
        musicAlbumRepository.save(album);
        storageRefService.bind(album.getCoverFileId(), StorageRefService.BIZ_MUSIC, album.getId());
        return album.getId();
    }

    @Override
    @Transactional
    public void albumUpdate(Long id, MusicAlbumSaveRequest request) {
        MusicAlbum album = requireAlbum(id);
        Long oldCoverFileId = album.getCoverFileId();
        applyAlbumRequest(album, request);
        musicAlbumRepository.save(album);
        // 封面更换：旧引用解绑（归零自动清理），新引用登记
        if (oldCoverFileId != null && !oldCoverFileId.equals(album.getCoverFileId())) {
            storageRefService.unbindFile(oldCoverFileId, StorageRefService.BIZ_MUSIC, id);
        }
        if (album.getCoverFileId() != null && !album.getCoverFileId().equals(oldCoverFileId)) {
            storageRefService.bind(album.getCoverFileId(), StorageRefService.BIZ_MUSIC, id);
        }
    }

    @Override
    @Transactional
    public void albumDelete(Long id) {
        requireAlbum(id);
        // 防呆：合集内仍有曲目时拒绝删除（曲目为独立业务数据，不允许静默游离/丢失）
        if (musicRepository.countByAlbumId(id) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "合集内仍有曲目，请先移除曲目再删除合集");
        }
        musicAlbumRepository.deleteById(id);
        storageRefService.unbindBiz(StorageRefService.BIZ_MUSIC, id);
    }

    // ===== 单曲管理 =====

    @Override
    public PageResult<MusicAdminVO> list(int page, int size, String keyword, Long albumId) {
        Specification<Music> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(cb.like(root.get("title"), pattern),
                        cb.like(root.get("artist"), pattern)));
            }
            if (albumId != null) {
                predicates.add(cb.equal(root.get("albumId"), albumId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Music> result = musicRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));
        List<MusicAdminVO> records = result.getContent().stream().map(this::toAdminVO).toList();
        return PageResult.of(records, result.getTotalElements(), page, size);
    }

    @Override
    @Transactional
    public Long create(MusicSaveRequest request) {
        Music music = new Music();
        applyMusicRequest(music, request);
        musicRepository.save(music);
        storageRefService.bind(music.getFileId(), StorageRefService.BIZ_MUSIC, music.getId());
        storageRefService.bind(music.getCoverFileId(), StorageRefService.BIZ_MUSIC, music.getId());
        return music.getId();
    }

    @Override
    @Transactional
    public void update(Long id, MusicSaveRequest request) {
        Music music = requireMusic(id);
        Long oldFileId = music.getFileId();
        Long oldCoverFileId = music.getCoverFileId();
        applyMusicRequest(music, request);
        musicRepository.save(music);
        // 文件/封面更换：旧引用解绑（归零自动清理），新引用登记
        if (!oldFileId.equals(music.getFileId())) {
            storageRefService.unbindFile(oldFileId, StorageRefService.BIZ_MUSIC, id);
            storageRefService.bind(music.getFileId(), StorageRefService.BIZ_MUSIC, id);
        }
        if (oldCoverFileId != null && !oldCoverFileId.equals(music.getCoverFileId())) {
            storageRefService.unbindFile(oldCoverFileId, StorageRefService.BIZ_MUSIC, id);
        }
        if (music.getCoverFileId() != null && !music.getCoverFileId().equals(oldCoverFileId)) {
            storageRefService.bind(music.getCoverFileId(), StorageRefService.BIZ_MUSIC, id);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireMusic(id);
        musicRepository.deleteById(id);
        storageRefService.unbindBiz(StorageRefService.BIZ_MUSIC, id);
    }

    @Override
    @Transactional
    public String generateEffect(Long id) {
        Music music = requireMusic(id);
        String config = effectConfigService.generate(music.getCoverFileId(), music.getDuration());
        music.setEffectConfig(config);
        music.setEffectSource(1);
        musicRepository.save(music);
        return config;
    }

    // ===== 入参应用与校验 =====

    /**
     * 合集入参应用（固定合集必须提供认证信息）
     */
    private void applyAlbumRequest(MusicAlbum album, MusicAlbumSaveRequest request) {
        album.setTitle(request.title().trim());
        album.setIntro(request.intro());
        album.setCoverFileId(requireValidFileId(request.coverFileId()));
        album.setType(request.type());
        album.setCertification(request.certification());
        if (request.type() == TYPE_CERTIFIED
                && (request.certification() == null || request.certification().isBlank())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "固定合集必须填写认证信息");
        }
        album.setIsRecommend(request.isRecommend() == null ? 0 : request.isRecommend());
    }

    /**
     * 单曲入参应用（音频文件防呆 + 时长取值策略 + 特效配置校验）
     */
    private void applyMusicRequest(Music music, MusicSaveRequest request) {
        music.setTitle(request.title().trim());
        music.setArtist(request.artist());
        if (request.albumId() != null) {
            requireAlbum(request.albumId());
        }
        music.setAlbumId(request.albumId());
        music.setCoverFileId(requireValidFileId(request.coverFileId()));
        StorageFile file = requireAudioFile(request.fileId());
        music.setFileId(file.getId());
        music.setLyricText(request.lyricText());
        music.setLyricOffset(request.lyricOffset() == null ? 0 : request.lyricOffset());
        // 时长：请求显式补录优先（探测失败场景），否则取 storage_file 探测值
        music.setDuration(request.duration() != null && request.duration() > 0
                ? request.duration()
                : file.getDuration() == null ? 0 : file.getDuration());
        // 特效配置：提交即校验（手工调参 source=2；生成端点另行走 generateEffect）
        if (request.effectConfig() != null && !request.effectConfig().isBlank()) {
            String error = EffectConfigValidator.validate(request.effectConfig());
            if (error != null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "特效配置未通过 Schema 校验: " + error);
            }
            music.setEffectConfig(request.effectConfig());
            music.setEffectSource(request.effectSource() == null ? 2 : request.effectSource());
        }
        music.setIsRecommend(request.isRecommend() == null ? 0 : request.isRecommend());
    }

    /**
     * 校验音频文件：存在、状态正常、且为音频类型（§8.4 白名单）
     */
    private StorageFile requireAudioFile(Long fileId) {
        StorageFile file = requireValidFile(fileId);
        if (!FileTypeValidator.isAudio(file.getExt())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "文件不是有效音频（仅支持 mp3/flac/wav/aac/m4a）");
        }
        return file;
    }

    /**
     * 校验文件 ID 指向正常状态文件（防呆：不允许引用已删除/不存在的文件）
     *
     * @return 文件元数据；fileId 为空返回 null（封面可空）
     */
    private StorageFile requireValidFile(Long fileId) {
        if (fileId == null) {
            return null;
        }
        return storageFileRepository.findById(fileId)
                .filter(file -> file.getStatus() == 1)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "文件不存在"));
    }

    /**
     * 封面文件 ID 防呆校验（可空）
     */
    private Long requireValidFileId(Long fileId) {
        requireValidFile(fileId);
        return fileId;
    }

    private MusicAlbum requireAlbum(Long id) {
        return musicAlbumRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MUSIC_ALBUM_NOT_FOUND));
    }

    private Music requireMusic(Long id) {
        return musicRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MUSIC_NOT_FOUND));
    }

    /**
     * 合集管理 VO 转换（trackCount group by 批量聚合，避免逐行 count 的 N+1）
     */
    private List<MusicAlbumAdminVO> toAlbumAdminVOs(List<MusicAlbum> albums) {
        Map<Long, Long> counts = albums.isEmpty() ? Map.of()
                : musicRepository.countGroupByAlbumId(albums.stream().map(MusicAlbum::getId).toList())
                        .stream().collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        return albums.stream().map(album -> new MusicAlbumAdminVO(album.getId(), album.getTitle(),
                album.getCoverFileId(),
                album.getCoverFileId() == null ? null : fileUploadService.signUrl(album.getCoverFileId()),
                album.getIntro(), album.getType(), album.getCertification(), album.getIsRecommend(),
                counts.getOrDefault(album.getId(), 0L),
                album.getCreateTime(), album.getUpdateTime())).toList();
    }

    private MusicAdminVO toAdminVO(Music music) {
        return new MusicAdminVO(music.getId(), music.getTitle(), music.getArtist(), music.getAlbumId(),
                music.getCoverFileId(),
                music.getCoverFileId() == null ? null : fileUploadService.signUrl(music.getCoverFileId()),
                music.getFileId(), fileUploadService.signUrl(music.getFileId()),
                music.getLyricText(), music.getLyricOffset(), music.getDuration(),
                music.getEffectConfig(), music.getEffectSource(), music.getIsRecommend(),
                music.getCreateTime(), music.getUpdateTime());
    }
}
