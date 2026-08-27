package top.heyqing.aether.service.impl.album;

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
import top.heyqing.aether.model.entity.Album;
import top.heyqing.aether.model.entity.AlbumImage;
import top.heyqing.aether.model.entity.StorageFile;
import top.heyqing.aether.model.vo.AlbumDetailVO;
import top.heyqing.aether.model.vo.AlbumImageVO;
import top.heyqing.aether.model.vo.AlbumListVO;
import top.heyqing.aether.repository.AlbumImageRepository;
import top.heyqing.aether.repository.AlbumRepository;
import top.heyqing.aether.repository.StorageFileRepository;
import top.heyqing.aether.service.album.AlbumService;
import top.heyqing.aether.service.storage.FileUploadService;

/**
 * 图集公开服务实现（BackEnd-Plan §5.2 GET /albums）
 *
 * <p>图片宽高/大小来自 storage_file 元数据（预览层悬浮信息展示用，UI-Plan §6.4）；
 * 全部媒体地址走签名 URL（§4.4 防直链）。</p>
 */
@Service
public class AlbumServiceImpl implements AlbumService {

    private final AlbumRepository albumRepository;
    private final AlbumImageRepository albumImageRepository;
    private final StorageFileRepository storageFileRepository;
    private final FileUploadService fileUploadService;

    public AlbumServiceImpl(AlbumRepository albumRepository, AlbumImageRepository albumImageRepository,
                            StorageFileRepository storageFileRepository, FileUploadService fileUploadService) {
        this.albumRepository = albumRepository;
        this.albumImageRepository = albumImageRepository;
        this.storageFileRepository = storageFileRepository;
        this.fileUploadService = fileUploadService;
    }

    @Override
    public PageResult<AlbumListVO> pageList(int page, int size, String keyword) {
        Specification<Album> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(cb.like(root.get("title"), pattern),
                        cb.like(root.get("intro"), pattern)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Album> result = albumRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));
        List<AlbumListVO> records = toListVOs(result.getContent());
        return PageResult.of(records, result.getTotalElements(), page, size);
    }

    @Override
    public List<AlbumListVO> recommend(int limit) {
        List<Album> albums = albumRepository.findByIsRecommendOrderBySortDescIdDesc(1,
                PageRequest.of(0, Math.min(Math.max(limit, 1), 20)));
        return toListVOs(albums);
    }

    @Override
    public AlbumDetailVO detail(Long id) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALBUM_NOT_FOUND));
        List<AlbumImage> images = albumImageRepository.findByAlbumIdOrderBySortAscIdAsc(id);
        // 图片文件元数据一次取齐（宽高/大小）
        Map<Long, StorageFile> files = images.isEmpty() ? Map.of()
                : storageFileRepository.findAllById(images.stream().map(AlbumImage::getFileId).toList())
                        .stream().collect(Collectors.toMap(StorageFile::getId, Function.identity()));
        List<AlbumImageVO> imageVOs = images.stream().map(image -> {
            StorageFile file = files.get(image.getFileId());
            return new AlbumImageVO(image.getId(), image.getTitle(), image.getIntro(),
                    fileUploadService.signUrl(image.getFileId()),
                    file == null ? null : file.getWidth(),
                    file == null ? null : file.getHeight(),
                    file == null ? 0 : file.getSize());
        }).toList();
        return new AlbumDetailVO(album.getId(), album.getTitle(),
                album.getCoverFileId() == null ? null : fileUploadService.signUrl(album.getCoverFileId()),
                album.getIntro(), album.getCreateTime(), imageVOs);
    }

    /**
     * 列表 VO 转换（imageCount 用 group by 一次取齐，避免 N+1）
     */
    private List<AlbumListVO> toListVOs(List<Album> albums) {
        Map<Long, Long> counts = albums.isEmpty() ? Map.of()
                : albumImageRepository.countGroupByAlbumId(albums.stream().map(Album::getId).toList())
                        .stream().collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        return albums.stream().map(album -> new AlbumListVO(album.getId(), album.getTitle(),
                album.getCoverFileId() == null ? null : fileUploadService.signUrl(album.getCoverFileId()),
                album.getIntro(), counts.getOrDefault(album.getId(), 0L))).toList();
    }
}
