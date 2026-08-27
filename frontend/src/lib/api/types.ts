/**
 * 后端 VO 类型（与 BackEnd-Plan §5.2 / java record 一一对应）
 */

/** 统一返回体（BackEnd-Plan §3.1） */
export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
  requestId: string;
  timestamp: number;
}

/** 分页结构（BackEnd-Plan §3.1） */
export interface PageResult<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
}

/** 分类 VO */
export interface CategoryVO {
  id: number;
  name: string;
  slug: string;
  sort: number;
}

/** 标签 VO */
export interface TagVO {
  id: number;
  name: string;
  slug: string;
}

/** 文章列表 VO（BackEnd-Plan §5.2，不返回创建时间） */
export interface ArticleListVO {
  id: number;
  title: string;
  summary: string | null;
  coverUrl: string | null;
  readingCount: number;
  isHot: number;
  publishTime: string;
  categories: CategoryVO[];
  tags: TagVO[];
}

/** 文章样式 VO（style_json 结构见 BackEnd-Plan §6.3） */
export interface ArticleStyleVO {
  id: number;
  name: string;
  styleJson: string;
  isDefault: number;
}

/** 文章详情 VO */
export interface ArticleDetailVO {
  id: number;
  title: string;
  summary: string | null;
  coverUrl: string | null;
  contentHtml: string;
  wordCount: number;
  readingCount: number;
  isHot: number;
  publishTime: string;
  updateTime: string;
  style: ArticleStyleVO | null;
  categories: CategoryVO[];
  tags: TagVO[];
}

/** 文章独立样式解析结构（style_json 反序列化，BackEnd-Plan §6.3） */
export interface ArticleStyleConfig {
  fontFamily?: string;
  fontSize?: number;
  lineHeight?: number;
  letterSpacing?: number;
  wordSpacing?: number;
  paragraphSpacing?: number;
  firstLineIndent?: string;
  contentWidth?: number;
  themeColor?: string;
  serif?: boolean;
  customCss?: string;
}
