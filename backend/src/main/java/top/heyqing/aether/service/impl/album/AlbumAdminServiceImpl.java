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
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.exception.BusinessException;
import top.heyqing.aether.model.dto.AlbumImageBatchRequest;
import top.heyqing.aether.model.dto.AlbumSaveRequest;
import top.heyqing.aether.model.entity.Album;
import top.heyqing.aether.model.entity.AlbumImage;
import top.heyqing.aether.model.entity.StorageFile;
import top.heyqing.aether.model.vo.AlbumAdminVO;
import top.heyqing.aether.repository.AlbumImageRepository;
import top.heyqing.aether.repository.AlbumRepository;
import top.heyqing.aether.repository.StorageFileRepository;
import top.heyqing.aether.service.album.AlbumAdminService;
import top.heyqing.aether.service.storage.FileUploadService;
import top.heyqing.aether.service.storage.StorageRefService;
import top.heyqing.aether.storage.FileTypeValidator;

/**
 * 图集管理实现（BackEnd-Plan §5.2 /admin/albums）
 *
 * <p>封面/图片文件引用经 {@link StorageRefService} 登记（bizType=album）；
 * 删除图集时引用归零的文件标记待清理并异步物理删除（§8.3）。</p>
 */
@Service
public class AlbumAdminServiceImpl implements AlbumAdminService {

    private final AlbumRepository albumRepository;
    private final AlbumImageRepository albumImageRepository;
    private final StorageFileRepository storageFileRepository;
    private final FileUploadService fileUploadService;
    private final StorageRefService storageRefService;

    public AlbumAdminServiceImpl(AlbumRepository albumRepository, AlbumImageRepository albumImageRepository,
                                 StorageFileRepository storageFileRepository, FileUploadService fileUploadService,
                                 StorageRefService storageRefService) {
        this.albumRepository = albumRepository;
        this.albumImageRepository = albumImageRepository;
        this.storageFileRepository = storageFileRepository;
        this.fileUploadService = fileUploadService;
        this.storageRefService = storageRefService;
    }

    @Override
    public PageResult<AlbumAdminVO> list(int page, int size, String keyword) {
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
        List<AlbumAdminVO> records = toAdminVOs(result.getContent());
        return PageResult.of(records, result.getTotalElements(), page, size);
    }

    @Override
    @Transactional
    public Long create(AlbumSaveRequest request) {
        Album album = new Album();
        applyRequest(album, request);
        albumRepository.save(album);
        storageRefService.bind(album.getCoverFileId(), StorageRefService.BIZ_ALBUM, album.getId());
        return album.getId();
    }

    @Override
    @Transactional
    public void update(Long id, AlbumSaveRequest request) {
        Album album = requireAlbum(id);
        Long oldCoverFileId = album.getCoverFileId();
        applyRequest(album, request);
        albumRepository.save(album);
        // 封面更换：旧引用解绑（归零自动清理），新引用登记
        if (oldCoverFileId != null && !oldCoverFileId.equals(album.getCoverFileId())) {
            storageRefService.unbindFile(oldCoverFileId, StorageRefService.BIZ_ALBUM, id);
        }
        if (album.getCoverFileId() != null && !album.getCoverFileId().equals(oldCoverFileId)) {
            storageRefService.bind(album.getCoverFileId(), StorageRefService.BIZ_ALBUM, id);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireAlbum(id);
        albumRepository.deleteById(id);
        albumImageRepository.deleteByAlbumId(id);
        // 引用整体解绑：封面与图片文件引用归零者标记待清理（提交后异步物理删除）
        storageRefService.unbindBiz(StorageRefService.BIZ_ALBUM, id);
    }

    @Override
    @Transactional
    public void addImages(Long albumId, AlbumImageBatchRequest request) {
        requireAlbum(albumId);
        // 防呆：全部文件必须存在且为图片类型（防止视频/文本文件混入图集）
        List<Long> fileIds = request.items().stream().map(AlbumImageBatchRequest.Item::fileId).distinct().toList();
        Map<Long, StorageFile> files = storageFileRepository.findAllById(fileIds).stream()
                .collect(Collectors.toMap(StorageFile::getId, Function.identity()));
        if (files.size() != fileIds.size()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "存在无效的图片文件 ID");
        }
        files.values().forEach(file -> {
            if (file.getStatus() != 1 || !FileTypeValidator.isImage(file.getExt())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "文件不是有效图片: " + file.getOriginalName());
            }
        });
        // 排序号：显式传入优先，缺省从当前最大 sort+1 递增
        List<AlbumImage> existing = albumImageRepository.findByAlbumIdOrderBySortAscIdAsc(albumId);
        int nextSort = existing.isEmpty() ? 0
                : existing.stream().mapToInt(AlbumImage::getSort).max().orElse(0) + 1;
        for (AlbumImageBatchRequest.Item item : request.items()) {
            AlbumImage image = new AlbumImage();
            image.setAlbumId(albumId);
            image.setFileId(item.fileId());
            image.setTitle(item.title());
            image.setIntro(item.intro());
            image.setSort(item.sort() == null ? nextSort : item.sort());
            albumImageRepository.save(image);
            storageRefService.bind(item.fileId(), StorageRefService.BIZ_ALBUM, albumId);
            nextSort = image.getSort() + 1;
        }
    }

    @Override
    @Transactional
    public void deleteImage(Long albumId, Long imageId) {
        AlbumImage image = albumImageRepository.findById(imageId)
                .filter(item -> item.getAlbumId().equals(albumId))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "图片不存在"));
        albumImageRepository.delete(image);
        // 引用解绑：同一文件在封面等多处引用时只减计数，归零才物理清理
        storageRefService.unbindFile(image.getFileId(), StorageRefService.BIZ_ALBUM, albumId);
    }

    /**
     * 入参应用到实体（封面文件防呆校验：必须存在且状态正常）
     */
    private void applyRequest(Album album, AlbumSaveRequest request) {
        album.setTitle(request.title().trim());
        album.setIntro(request.intro());
        album.setCoverFileId(requireValidFile(request.coverFileId()));
        album.setIsRecommend(request.isRecommend() == null ? 0 : request.isRecommend());
        album.setSort(request.sort() == null ? 0 : request.sort());
    }

    /**
     * 校验文件 ID 指向正常状态文件（防呆：不允许引用已删除/不存在的文件）
     */
    private Long requireValidFile(Long fileId) {
        if (fileId == null) {
            return null;
        }
        storageFileRepository.findById(fileId)
                .filter(file -> file.getStatus() == 1)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "封面文件不存在"));
        return fileId;
    }

    private Album requireAlbum(Long id) {
        return albumRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALBUM_NOT_FOUND));
    }

    private List<AlbumAdminVO> toAdminVOs(List<Album> albums) {
        Map<Long, Long> counts = albums.isEmpty() ? Map.of()
                : albumImageRepository.countGroupByAlbumId(albums.stream().map(Album::getId).toList())
                        .stream().collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        return albums.stream().map(album -> new AlbumAdminVO(album.getId(), album.getTitle(), album.getIntro(),
                album.getCoverFileId(),
                album.getCoverFileId() == null ? null : fileUploadService.signUrl(album.getCoverFileId()),
                album.getIsRecommend(), album.getSort(), counts.getOrDefault(album.getId(), 0L),
                album.getCreateTime(), album.getUpdateTime())).toList();
    }
}
