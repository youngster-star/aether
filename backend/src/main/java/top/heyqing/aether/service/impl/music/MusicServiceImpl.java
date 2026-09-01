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

import jakarta.persistence.criteria.Predicate;
import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.exception.BusinessException;
import top.heyqing.aether.model.entity.Music;
import top.heyqing.aether.model.entity.MusicAlbum;
import top.heyqing.aether.model.vo.MusicAlbumDetailVO;
import top.heyqing.aether.model.vo.MusicAlbumListVO;
import top.heyqing.aether.model.vo.MusicDetailVO;
import top.heyqing.aether.model.vo.MusicListVO;
import top.heyqing.aether.repository.MusicAlbumRepository;
import top.heyqing.aether.repository.MusicRepository;
import top.heyqing.aether.service.music.MusicService;
import top.heyqing.aether.service.storage.FileUploadService;

/**
 * 音乐公开服务实现（BackEnd-Plan §5.2 GET /music*）
 *
 * <p>合集封面/曲目封面/播放地址全部签名 URL；合集列表曲目数走
 * group by 批量统计避免 N+1；歌词与特效配置原样透出（前端解析渲染）。</p>
 */
@Service
public class MusicServiceImpl implements MusicService {

    private final MusicRepository musicRepository;
    private final MusicAlbumRepository musicAlbumRepository;
    private final FileUploadService fileUploadService;

    public MusicServiceImpl(MusicRepository musicRepository, MusicAlbumRepository musicAlbumRepository,
                            FileUploadService fileUploadService) {
        this.musicRepository = musicRepository;
        this.musicAlbumRepository = musicAlbumRepository;
        this.fileUploadService = fileUploadService;
    }

    @Override
    public PageResult<MusicAlbumListVO> pageAlbums(Integer type, int page, int size) {
        Specification<MusicAlbum> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<MusicAlbum> result = musicAlbumRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));
        List<MusicAlbumListVO> records = toAlbumListVOs(result.getContent());
        return PageResult.of(records, result.getTotalElements(), page, size);
    }

    @Override
    public MusicAlbumDetailVO albumDetail(Long id) {
        MusicAlbum album = requireAlbum(id);
        List<MusicListVO> tracks = musicRepository.findByAlbumIdOrderByIdAsc(id).stream()
                .map(this::toListVO)
                .toList();
        return new MusicAlbumDetailVO(album.getId(), album.getTitle(),
                album.getCoverFileId() == null ? null : fileUploadService.signUrl(album.getCoverFileId()),
                album.getIntro(), album.getType(), album.getCertification(), tracks);
    }

    @Override
    public PageResult<MusicListVO> search(String keyword, Long albumId, int page, int size) {
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
        List<MusicListVO> records = result.getContent().stream().map(this::toListVO).toList();
        return PageResult.of(records, result.getTotalElements(), page, size);
    }

    @Override
    public MusicDetailVO detail(Long id) {
        Music music = musicRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MUSIC_NOT_FOUND));
        String albumTitle = music.getAlbumId() == null ? null
                : musicAlbumRepository.findById(music.getAlbumId())
                        .map(MusicAlbum::getTitle).orElse(null);
        return new MusicDetailVO(music.getId(), music.getTitle(), music.getArtist(), music.getAlbumId(),
                albumTitle,
                music.getCoverFileId() == null ? null : fileUploadService.signUrl(music.getCoverFileId()),
                fileUploadService.signUrl(music.getFileId()), music.getLyricText(), music.getLyricOffset(),
                music.getDuration(), music.getEffectConfig(), music.getEffectSource());
    }

    @Override
    public List<MusicAlbumListVO> recommend(int limit) {
        List<MusicAlbum> albums = musicAlbumRepository.findByIsRecommend(1,
                PageRequest.of(0, Math.min(Math.max(limit, 1), 20), Sort.by(Sort.Direction.DESC, "id")));
        return toAlbumListVOs(albums);
    }

    /**
     * 合集列表 VO 转换（trackCount group by 一次取齐，避免 N+1）
     */
    private List<MusicAlbumListVO> toAlbumListVOs(List<MusicAlbum> albums) {
        Map<Long, Long> counts = albums.isEmpty() ? Map.of()
                : musicRepository.countGroupByAlbumId(albums.stream().map(MusicAlbum::getId).toList())
                        .stream().collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        return albums.stream().map(album -> new MusicAlbumListVO(album.getId(), album.getTitle(),
                album.getCoverFileId() == null ? null : fileUploadService.signUrl(album.getCoverFileId()),
                album.getIntro(), album.getType(), album.getCertification(),
                counts.getOrDefault(album.getId(), 0L))).toList();
    }

    private MusicListVO toListVO(Music music) {
        return new MusicListVO(music.getId(), music.getTitle(), music.getArtist(),
                music.getCoverFileId() == null ? null : fileUploadService.signUrl(music.getCoverFileId()),
                fileUploadService.signUrl(music.getFileId()),
                music.getDuration(), music.getAlbumId());
    }

    private MusicAlbum requireAlbum(Long id) {
        return musicAlbumRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MUSIC_ALBUM_NOT_FOUND));
    }
}
