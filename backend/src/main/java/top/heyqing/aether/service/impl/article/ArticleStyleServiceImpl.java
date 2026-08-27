package top.heyqing.aether.service.impl.article;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.exception.BusinessException;
import top.heyqing.aether.model.dto.ArticleStyleRequest;
import top.heyqing.aether.model.entity.ArticleStyle;
import top.heyqing.aether.model.vo.ArticleStyleVO;
import top.heyqing.aether.repository.ArticleRepository;
import top.heyqing.aether.repository.ArticleStyleRepository;
import top.heyqing.aether.service.article.ArticleStyleService;

/**
 * 文章样式管理实现（BackEnd-Plan §5.2 /admin/article-styles）
 */
@Service
public class ArticleStyleServiceImpl implements ArticleStyleService {

    private final ArticleStyleRepository articleStyleRepository;
    private final ArticleRepository articleRepository;

    public ArticleStyleServiceImpl(ArticleStyleRepository articleStyleRepository,
                                   ArticleRepository articleRepository) {
        this.articleStyleRepository = articleStyleRepository;
        this.articleRepository = articleRepository;
    }

    @Override
    public List<ArticleStyleVO> list() {
        return articleStyleRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(s -> new ArticleStyleVO(s.getId(), s.getName(), s.getStyleJson(), s.getIsDefault()))
                .toList();
    }

    @Override
    @Transactional
    public Long create(ArticleStyleRequest request) {
        ArticleStyle style = new ArticleStyle();
        applyRequest(style, request);
        if (request.isDefault() != null && request.isDefault() == 1) {
            clearDefault();
        }
        articleStyleRepository.save(style);
        return style.getId();
    }

    @Override
    @Transactional
    public void update(Long id, ArticleStyleRequest request) {
        ArticleStyle style = articleStyleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.STYLE_NOT_FOUND));
        applyRequest(style, request);
        if (request.isDefault() != null && request.isDefault() == 1) {
            clearDefault();
        }
        articleStyleRepository.save(style);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ArticleStyle style = articleStyleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.STYLE_NOT_FOUND));
        if (style.getIsDefault() == 1) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "默认样式无法删除，请先设置其他样式为默认");
        }
        if (articleRepository.existsByArticleStyleId(id)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "样式已被文章使用，无法删除");
        }
        articleStyleRepository.delete(style);
    }

    private void applyRequest(ArticleStyle style, ArticleStyleRequest request) {
        style.setName(request.name().trim());
        style.setStyleJson(request.styleJson().trim());
        style.setIsDefault(request.isDefault() == null ? 0 : request.isDefault());
    }

    /**
     * 取消现有默认样式（设置新默认前调用，保证全站默认唯一）
     */
    private void clearDefault() {
        articleStyleRepository.findByIsDefault(1)
                .ifPresent(defaultStyle -> {
                    defaultStyle.setIsDefault(0);
                    articleStyleRepository.save(defaultStyle);
                });
    }
}
