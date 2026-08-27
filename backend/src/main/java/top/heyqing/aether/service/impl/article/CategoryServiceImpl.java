package top.heyqing.aether.service.impl.article;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.exception.BusinessException;
import top.heyqing.aether.model.dto.CategorySaveRequest;
import top.heyqing.aether.model.dto.TagSaveRequest;
import top.heyqing.aether.model.entity.Category;
import top.heyqing.aether.model.entity.Tag;
import top.heyqing.aether.model.vo.CategoryVO;
import top.heyqing.aether.model.vo.TagVO;
import top.heyqing.aether.repository.BizCategoryRelRepository;
import top.heyqing.aether.repository.BizTagRelRepository;
import top.heyqing.aether.repository.CategoryRepository;
import top.heyqing.aether.repository.TagRepository;
import top.heyqing.aether.service.article.CategoryService;

/**
 * 分类/标签字典管理实现（BackEnd-Plan §5.2 /admin/categories、/admin/tags）
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final BizCategoryRelRepository bizCategoryRelRepository;
    private final BizTagRelRepository bizTagRelRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, TagRepository tagRepository,
                               BizCategoryRelRepository bizCategoryRelRepository,
                               BizTagRelRepository bizTagRelRepository) {
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.bizCategoryRelRepository = bizCategoryRelRepository;
        this.bizTagRelRepository = bizTagRelRepository;
    }

    @Override
    @Transactional
    public Long createCategory(CategorySaveRequest request) {
        if (categoryRepository.existsByBizTypeAndSlug(request.bizType(), request.slug())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "该业务域下唯一标识已存在");
        }
        Category category = new Category();
        category.setName(request.name().trim());
        category.setSlug(request.slug().trim());
        category.setBizType(request.bizType().trim());
        category.setSort(request.sort() == null ? 0 : request.sort());
        categoryRepository.save(category);
        return category.getId();
    }

    @Override
    @Transactional
    public void updateCategory(Long id, CategorySaveRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        // slug 冲突校验：排除自身
        boolean conflict = categoryRepository.findByBizTypeOrderBySortAsc(request.bizType()).stream()
                .anyMatch(c -> c.getSlug().equals(request.slug().trim()) && !c.getId().equals(id));
        if (conflict) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "该业务域下唯一标识已存在");
        }
        category.setName(request.name().trim());
        category.setSlug(request.slug().trim());
        category.setBizType(request.bizType().trim());
        category.setSort(request.sort() == null ? 0 : request.sort());
        categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        // 被业务引用时拒绝删除（防呆，避免孤儿关联）
        if (bizCategoryRelRepository.existsByBizTypeAndCategoryId("article", id)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "分类已被文章使用，无法删除");
        }
        categoryRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Long createTag(TagSaveRequest request) {
        if (tagRepository.existsByBizTypeAndSlug(request.bizType(), request.slug())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "该业务域下唯一标识已存在");
        }
        Tag tag = new Tag();
        tag.setName(request.name().trim());
        tag.setSlug(request.slug().trim());
        tag.setBizType(request.bizType().trim());
        tagRepository.save(tag);
        return tag.getId();
    }

    @Override
    @Transactional
    public void updateTag(Long id, TagSaveRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TAG_NOT_FOUND));
        boolean conflict = tagRepository.findByBizType(request.bizType()).stream()
                .anyMatch(t -> t.getSlug().equals(request.slug().trim()) && !t.getId().equals(id));
        if (conflict) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "该业务域下唯一标识已存在");
        }
        tag.setName(request.name().trim());
        tag.setSlug(request.slug().trim());
        tag.setBizType(request.bizType().trim());
        tagRepository.save(tag);
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
        if (bizTagRelRepository.existsByBizTypeAndTagId("article", id)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "标签已被文章使用，无法删除");
        }
        tagRepository.deleteById(id);
    }

    @Override
    public List<CategoryVO> listCategories(String bizType) {
        return categoryRepository.findByBizTypeOrderBySortAsc(bizType).stream()
                .map(c -> new CategoryVO(c.getId(), c.getName(), c.getSlug(), c.getSort()))
                .toList();
    }

    @Override
    public List<TagVO> listTags(String bizType) {
        return tagRepository.findByBizType(bizType).stream()
                .map(t -> new TagVO(t.getId(), t.getName(), t.getSlug()))
                .toList();
    }
}
