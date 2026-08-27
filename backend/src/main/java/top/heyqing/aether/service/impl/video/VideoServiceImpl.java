package top.heyqing.aether.service.impl.video;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.exception.BusinessException;
import top.heyqing.aether.model.entity.StorageFile;
import top.heyqing.aether.model.entity.Video;
import top.heyqing.aether.model.vo.VideoChapterVO;
import top.heyqing.aether.model.vo.VideoDetailVO;
import top.heyqing.aether.model.vo.VideoListVO;
import top.heyqing.aether.repository.StorageFileRepository;
import top.heyqing.aether.repository.VideoChapterRepository;
import top.heyqing.aether.repository.VideoRepository;
import top.heyqing.aether.service.storage.FileUploadService;
import top.heyqing.aether.service.video.VideoService;

/**
 * 视频公开服务实现（BackEnd-Plan §5.2 GET /videos）
 *
 * <p>播放地址为签名 URL（10 分钟有效，播放器每次拉取详情刷新，§4.4）；
 * 关键时间节点按 sort 排序返回。</p>
 */
@Service
public class VideoServiceImpl implements VideoService {

    private final VideoRepository videoRepository;
    private final VideoChapterRepository videoChapterRepository;
    private final StorageFileRepository storageFileRepository;
    private final FileUploadService fileUploadService;

    public VideoServiceImpl(VideoRepository videoRepository, VideoChapterRepository videoChapterRepository,
                            StorageFileRepository storageFileRepository, FileUploadService fileUploadService) {
        this.videoRepository = videoRepository;
        this.videoChapterRepository = videoChapterRepository;
        this.storageFileRepository = storageFileRepository;
        this.fileUploadService = fileUploadService;
    }

    @Override
    public PageResult<VideoListVO> pageList(int page, int size, String keyword) {
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
        List<VideoListVO> records = result.getContent().stream()
                .map(video -> new VideoListVO(video.getId(), video.getTitle(),
                        video.getCoverFileId() == null ? null : fileUploadService.signUrl(video.getCoverFileId()),
                        video.getIntro(), video.getDuration()))
                .toList();
        return PageResult.of(records, result.getTotalElements(), page, size);
    }

    @Override
    public VideoDetailVO detail(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
        List<VideoChapterVO> chapters = videoChapterRepository.findByVideoIdOrderBySortAscIdAsc(id).stream()
                .map(chapter -> new VideoChapterVO(chapter.getId(), chapter.getTitle(), chapter.getTimeOffset()))
                .toList();
        // 文件扩展名（播放器 type 判定：mp4/webm）
        String ext = storageFileRepository.findById(video.getFileId())
                .map(StorageFile::getExt)
                .orElse("mp4");
        return new VideoDetailVO(video.getId(), video.getTitle(),
                video.getCoverFileId() == null ? null : fileUploadService.signUrl(video.getCoverFileId()),
                video.getIntro(), video.getDuration(), fileUploadService.signUrl(video.getFileId()), ext,
                chapters);
    }
}
