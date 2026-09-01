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

/** 图集列表 VO（BackEnd-Plan §5.2 GET /albums） */
export interface AlbumListVO {
  id: number;
  title: string;
  coverUrl: string | null;
  intro: string | null;
  imageCount: number;
}

/** 图集图片 VO（图集详情内嵌） */
export interface AlbumImageVO {
  id: number;
  title: string | null;
  intro: string | null;
  url: string;
  width: number | null;
  height: number | null;
  size: number;
}

/** 图集详情 VO（BackEnd-Plan §5.2 GET /albums/{id}） */
export interface AlbumDetailVO {
  id: number;
  title: string;
  coverUrl: string | null;
  intro: string | null;
  createTime: string;
  images: AlbumImageVO[];
}

/** 视频列表 VO（BackEnd-Plan §5.2 GET /videos） */
export interface VideoListVO {
  id: number;
  title: string;
  coverUrl: string | null;
  intro: string | null;
  duration: number;
}

/** 视频关键时间节点 VO */
export interface VideoChapterVO {
  id: number;
  title: string;
  timeOffset: number;
}

/** 视频详情 VO（BackEnd-Plan §5.2 GET /videos/{id}） */
export interface VideoDetailVO {
  id: number;
  title: string;
  coverUrl: string | null;
  intro: string | null;
  duration: number;
  playUrl: string;
  /** 文件扩展名（mp4/webm，播放器类型判定用） */
  ext: string;
  chapters: VideoChapterVO[];
}

/** 音乐合集列表 VO（BackEnd-Plan §5.2 GET /music/albums） */
export interface MusicAlbumListVO {
  id: number;
  title: string;
  coverUrl: string | null;
  intro: string | null;
  /** 类型：1 自定义合集 2 固定合集 */
  type: number;
  /** 认证信息（固定合集展示，UI-Plan §6.6） */
  certification: string | null;
  trackCount: number;
}

/** 音乐单曲列表 VO（合集详情曲目/搜索/推荐通用） */
export interface MusicListVO {
  id: number;
  title: string;
  artist: string | null;
  coverUrl: string | null;
  /** 音频签名 URL（播放队列构建依赖，§4.4） */
  fileUrl: string;
  duration: number;
  albumId: number | null;
}

/** 音乐合集详情 VO（BackEnd-Plan §5.2 GET /music/albums/{id}） */
export interface MusicAlbumDetailVO {
  id: number;
  title: string;
  coverUrl: string | null;
  intro: string | null;
  type: number;
  certification: string | null;
  tracks: MusicListVO[];
}

/** 音乐单曲详情 VO（BackEnd-Plan §5.2 GET /music/{id}） */
export interface MusicDetailVO {
  id: number;
  title: string;
  artist: string | null;
  albumId: number | null;
  albumTitle: string | null;
  coverUrl: string | null;
  /** 音频签名 URL（§4.4 防直链） */
  fileUrl: string;
  /** 歌词文本（LRC 时间轴 / 纯文本，可空） */
  lyricText: string | null;
  /** 歌词全局偏移（毫秒，正负可调） */
  lyricOffset: number;
  duration: number;
  /** EffectConfig 原始 JSON（§7.3，可空=未生成） */
  effectConfig: string | null;
  /** 特效来源：1 生成 2 手工调整 */
  effectSource: number;
}
