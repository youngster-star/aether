package top.heyqing.aether.service.impl.video;

import java.util.ArrayList;
import java.util.List;

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
import top.heyqing.aether.model.dto.VideoChapterBatchRequest;
import top.heyqing.aether.model.dto.VideoSaveRequest;
import top.heyqing.aether.model.entity.StorageFile;
import top.heyqing.aether.model.entity.Video;
import top.heyqing.aether.model.entity.VideoChapter;
import top.heyqing.aether.model.vo.VideoAdminVO;
import top.heyqing.aether.model.vo.VideoChapterVO;
import top.heyqing.aether.repository.StorageFileRepository;
import top.heyqing.aether.repository.VideoChapterRepository;
import top.heyqing.aether.repository.VideoRepository;
import top.heyqing.aether.service.storage.FileUploadService;
import top.heyqing.aether.service.storage.StorageRefService;
import top.heyqing.aether.service.video.VideoAdminService;
import top.heyqing.aether.storage.FileTypeValidator;

/**
 * 视频管理实现（BackEnd-Plan §5.2 /admin/videos）
 *
 * <p>视频文件与封面引用经 {@link StorageRefService} 登记（bizType=video）；
 * 时长默认取 storage_file 探测值，探测失败时以请求值手工补录。</p>
 */
@Service
public class VideoAdminServiceImpl implements VideoAdminService {

    private final VideoRepository videoRepository;
    private final VideoChapterRepository videoChapterRepository;
    private final StorageFileRepository storageFileRepository;
    private final FileUploadService fileUploadService;
    private final StorageRefService storageRefService;

    public VideoAdminServiceImpl(VideoRepository videoRepository, VideoChapterRepository videoChapterRepository,
                                 StorageFileRepository storageFileRepository, FileUploadService fileUploadService,
                                 StorageRefService storageRefService) {
        this.videoRepository = videoRepository;
        this.videoChapterRepository = videoChapterRepository;
        this.storageFileRepository = storageFileRepository;
        this.fileUploadService = fileUploadService;
        this.storageRefService = storageRefService;
    }

    @Override
    public PageResult<VideoAdminVO> list(int page, int size, String keyword) {
        Specification<Video> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(cb.like(root.get("title"), pattern),
                        cb.like(root.get("intro"), pattern)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Video> result = videoRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));
        List<VideoAdminVO> records = result.getContent().stream().map(this::toAdminVO).toList();
        return PageResult.of(records, result.getTotalElements(), page, size);
    }

    @Override
    @Transactional
    public Long create(VideoSaveRequest request) {
        Video video = new Video();
        applyRequest(video, request);
        videoRepository.save(video);
        storageRefService.bind(video.getFileId(), StorageRefService.BIZ_VIDEO, video.getId());
        storageRefService.bind(video.getCoverFileId(), StorageRefService.BIZ_VIDEO, video.getId());
        return video.getId();
    }

    @Override
    @Transactional
    public void update(Long id, VideoSaveRequest request) {
        Video video = requireVideo(id);
        Long oldFileId = video.getFileId();
        Long oldCoverFileId = video.getCoverFileId();
        applyRequest(video, request);
        videoRepository.save(video);
        // 文件/封面更换：旧引用解绑（归零自动清理），新引用登记
        if (!oldFileId.equals(video.getFileId())) {
            storageRefService.unbindFile(oldFileId, StorageRefService.BIZ_VIDEO, id);
            storageRefService.bind(video.getFileId(), StorageRefService.BIZ_VIDEO, id);
        }
        if (oldCoverFileId != null && !oldCoverFileId.equals(video.getCoverFileId())) {
            storageRefService.unbindFile(oldCoverFileId, StorageRefService.BIZ_VIDEO, id);
        }
        if (video.getCoverFileId() != null && !video.getCoverFileId().equals(oldCoverFileId)) {
            storageRefService.bind(video.getCoverFileId(), StorageRefService.BIZ_VIDEO, id);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireVideo(id);
        videoRepository.deleteById(id);
        videoChapterRepository.deleteByVideoId(id);
        // 引用整体解绑：视频文件与封面引用归零者标记待清理（提交后异步物理删除）
        storageRefService.unbindBiz(StorageRefService.BIZ_VIDEO, id);
    }

    @Override
    @Transactional
    public void saveChapters(Long id, VideoChapterBatchRequest request) {
        requireVideo(id);
        // 整体替换：先清后插（空列表 = 清空全部节点）
        videoChapterRepository.deleteByVideoId(id);
        int sort = 0;
        for (VideoChapterBatchRequest.Item item : request.items()) {
            VideoChapter chapter = new VideoChapter();
            chapter.setVideoId(id);
            chapter.setTitle(item.title().trim());
            chapter.setTimeOffset(item.timeOffset());
            chapter.setSort(item.sort() == null ? sort : item.sort());
            videoChapterRepository.save(chapter);
            sort = chapter.getSort() + 1;
        }
    }

    /**
     * 入参应用到实体（文件防呆校验 + 时长取值策略）
     */
    private void applyRequest(Video video, VideoSaveRequest request) {
        video.setTitle(request.title().trim());
        video.setIntro(request.intro());
        video.setCoverFileId(requireValidFileId(request.coverFileId()));
        video.setFileId(requireVideoFileId(request.fileId()));
        // 时长：请求显式补录优先（探测失败场景），否则取 storage_file 探测值
        StorageFile file = storageFileRepository.findById(video.getFileId()).orElseThrow();
        video.setDuration(request.duration() != null && request.duration() > 0
                ? request.duration()
                : file.getDuration() == null ? 0 : file.getDuration());
        video.setIsRecommend(request.isRecommend() == null ? 0 : request.isRecommend());
    }

    /**
     * 校验视频文件：存在、状态正常、且为视频类型（mp4/webm，§8.4）
     */
    private Long requireVideoFileId(Long fileId) {
        StorageFile file = requireValidFile(fileId);
        if (!FileTypeValidator.isVideo(file.getExt())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不是有效视频（仅支持 mp4/webm）");
        }
        return fileId;
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

    private Video requireVideo(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
    }

    private VideoAdminVO toAdminVO(Video video) {
        List<VideoChapterVO> chapters = videoChapterRepository.findByVideoIdOrderBySortAscIdAsc(video.getId())
                .stream()
                .map(chapter -> new VideoChapterVO(chapter.getId(), chapter.getTitle(), chapter.getTimeOffset()))
                .toList();
        return new VideoAdminVO(video.getId(), video.getTitle(), video.getIntro(),
                video.getCoverFileId(),
                video.getCoverFileId() == null ? null : fileUploadService.signUrl(video.getCoverFileId()),
                video.getFileId(), fileUploadService.signUrl(video.getFileId()), video.getDuration(),
                video.getIsRecommend(), video.getCreateTime(), video.getUpdateTime(), chapters);
    }
}
