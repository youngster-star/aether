package top.heyqing.aether;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import top.heyqing.aether.config.StorageProperties;
import top.heyqing.aether.model.dto.AlbumImageBatchRequest;
import top.heyqing.aether.model.dto.AlbumSaveRequest;
import top.heyqing.aether.model.dto.StorageInitRequest;
import top.heyqing.aether.model.dto.VideoChapterBatchRequest;
import top.heyqing.aether.model.dto.VideoSaveRequest;
import top.heyqing.aether.model.entity.StorageFile;
import top.heyqing.aether.model.vo.StorageInitVO;
import top.heyqing.aether.model.vo.StorageMergeVO;
import top.heyqing.aether.repository.StorageFileRepository;
import top.heyqing.aether.repository.StorageRefRepository;
import top.heyqing.aether.service.album.AlbumAdminService;
import top.heyqing.aether.service.album.AlbumService;
import top.heyqing.aether.service.storage.FileUploadService;
import top.heyqing.aether.service.video.VideoAdminService;
import top.heyqing.aether.service.video.VideoService;
import top.heyqing.aether.util.DigestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 图集 + 视频模块功能测试（阶段 3 完成标准，BackEnd-Plan §5.2/§4.4/§4.5/§8.3）
 *
 * <p>基于 seed 数据（2 图集 6+2 图、2 视频含节点）验证：公开列表/推荐/详情、
 * 签名 URL 访问（无签名/错签名/过期 20001、正确 200、Range 206）、
 * 图片上传 EXIF 抹除（GPS 定位信息不落存储）、MP4 内置时长解析回退（本机无 ffprobe）、
 * 管理 CRUD 与引用归零删除一致性（storage_file.status=0 + storage_ref 清理）。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = "aether.storage.local-base-dir=target/test-storage")
class AlbumVideoFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlbumService albumService;

    @Autowired
    private AlbumAdminService albumAdminService;

    @Autowired
    private VideoService videoService;

    @Autowired
    private VideoAdminService videoAdminService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private StorageFileRepository storageFileRepository;

    @Autowired
    private StorageRefRepository storageRefRepository;

    @Autowired
    private StorageProperties storageProperties;

    // ===== 公开接口（seed 数据） =====

    @Test
    @DisplayName("图集公开接口：分页列表/推荐/详情（图片含宽高大小与签名 URL）")
    void albumsPublicApi() throws Exception {
        mockMvc.perform(get("/v1/albums"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].coverUrl").value(org.hamcrest.Matchers.containsString("sign=")))
                .andExpect(jsonPath("$.data.records[0].imageCount").value(org.hamcrest.Matchers.greaterThan(0)));

        mockMvc.perform(get("/v1/albums/recommend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2));

        Long albumId = albumService.pageList(1, 10, null).records().get(0).id();
        mockMvc.perform(get("/v1/albums/{id}", albumId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.images").isNotEmpty())
                .andExpect(jsonPath("$.data.images[0].url").value(org.hamcrest.Matchers.containsString("sign=")))
                .andExpect(jsonPath("$.data.images[0].width").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.images[0].height").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.images[0].size").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("视频公开接口：分页列表/详情（时长与关键时间节点）")
    void videosPublicApi() throws Exception {
        mockMvc.perform(get("/v1/videos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].duration").value(org.hamcrest.Matchers.greaterThan(0)));

        Long videoId = videoService.pageList(1, 10, null).records().get(0).id();
        mockMvc.perform(get("/v1/videos/{id}", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.playUrl").value(org.hamcrest.Matchers.containsString("sign=")))
                .andExpect(jsonPath("$.data.chapters").isNotEmpty())
                .andExpect(jsonPath("$.data.chapters[0].timeOffset").isNumber());
    }

    // ===== 签名 URL 访问（§4.4） =====

    @Test
    @DisplayName("签名访问：无签名/错签名/过期 20001，正确 200，Range 请求 206（视频拖动依赖）")
    void signedFileAccessAndRange() throws Exception {
        StorageFile file = seedImageFile();
        String signUrl = fileUploadService.signUrl(file.getId());
        String query = signUrl.substring(signUrl.indexOf('?') + 1);
        // 注意：查询串含 & = 特殊字符，不能走 MockMvc URI 模板变量（会被编码），直接拼接
        String path = "/v1/storage/file/" + file.getId() + "?" + query;

        // 正确签名 → 200
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("image/")));

        // 无签名参数 → 10001 参数缺失（未授权直链同样被拒）
        mockMvc.perform(get("/v1/storage/file/{fileId}", file.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10001));

        // 错误签名 → 20001
        mockMvc.perform(get("/v1/storage/file/{fileId}?expires=9999999999&sign=deadbeef", file.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20001));

        // 过期签名（签名本身正确但已过期）→ 20001
        long expired = System.currentTimeMillis() / 1000 - 1000;
        String expiredSign = DigestUtil.hmacSha256(file.getId() + ":" + expired, storageProperties.getSignSecret());
        mockMvc.perform(get("/v1/storage/file/" + file.getId() + "?expires=" + expired + "&sign=" + expiredSign))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20001));

        // Range：bytes=0-99 → 206 + Content-Range（视频进度条拖动依赖，BackEnd-Plan §4.4）
        mockMvc.perform(get(path).header("Range", "bytes=0-99"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range",
                        "bytes 0-99/" + file.getSize()));
    }

    // ===== EXIF 抹除（§4.5） =====

    @Test
    @DisplayName("图片上传 EXIF 抹除：带 GPS 的 JPEG 合并后 APP1 段消失且仍是合法 JPEG")
    void exifStrippedOnImageUpload() throws Exception {
        byte[] jpeg = jpegWithGps();
        StorageFile file = upload(jpeg, "gps-照片.jpg");
        byte[] stored = fileUploadService.openFile(file).readAllBytes();

        assertTrue(stored[0] == (byte) 0xFF && stored[1] == (byte) 0xD8, "抹除后仍是合法 JPEG");
        assertFalse(containsApp1(stored), "合并产物不应残留 EXIF APP1 段（GPS 定位信息）");
        assertNotNull(file.getWidth(), "宽高探测应正常");
    }

    // ===== MP4 时长回退探测（§8.2） =====

    @Test
    @DisplayName("MP4 时长探测回退：无 ffprobe 时内置 mvhd 解析出时长")
    void mp4DurationProbedWithoutFfprobe() {
        // 271 秒避开 seed 视频同构字节（300/180 秒）的秒传命中，确保走真实上传探测链路
        byte[] mp4 = minimalMp4(271);
        StorageFile file = upload(mp4, "测试视频.mp4");
        assertEquals(271, file.getDuration(), "本机无 ffprobe，应走内置 mvhd 解析回退");
        assertEquals("mp4", file.getExt());
    }

    // ===== 管理 CRUD + 删除一致性（§8.3） =====

    @Test
    @DisplayName("图集管理：CRUD/换封面/删图/删集，引用归零文件 status=0 且 storage_ref 清理")
    void albumAdminCrudAndDeleteConsistency() {
        StorageFile cover = upload(renderPng(0xB49A6E), "封面A.png");
        StorageFile cover2 = upload(renderPng(0x8FA3B8), "封面B.png");
        StorageFile photo = upload(renderPng(0xC9A86B), "图1.png");

        // 新建 + 批量加图
        Long albumId = albumAdminService.create(new AlbumSaveRequest("测试图集", "介绍", cover.getId(), 1, 5));
        albumAdminService.addImages(albumId, new AlbumImageBatchRequest(
                List.of(new AlbumImageBatchRequest.Item(photo.getId(), "图1", "介绍1", 0))));
        assertEquals(1, albumService.detail(albumId).images().size(), "批量加图后详情应有 1 张图");

        // 换封面：旧封面引用归零 → status=0
        albumAdminService.update(albumId, new AlbumSaveRequest("测试图集2", "介绍2", cover2.getId(), 0, 0));
        assertEquals(0, storageFileRepository.findById(cover.getId()).orElseThrow().getStatus(),
                "旧封面引用归零应标记待清理");

        // 删图：图片文件引用归零 → status=0
        Long imageId = albumService.detail(albumId).images().get(0).id();
        albumAdminService.deleteImage(albumId, imageId);
        assertEquals(0, storageFileRepository.findById(photo.getId()).orElseThrow().getStatus(),
                "图片引用归零应标记待清理");

        // 删集：新封面归零 + 引用表清空
        albumAdminService.delete(albumId);
        assertEquals(0, storageFileRepository.findById(cover2.getId()).orElseThrow().getStatus());
        assertTrue(storageRefRepository.findByBizTypeAndBizId("album", albumId).isEmpty(),
                "删除图集后 storage_ref 应无残留引用");
    }

    @Test
    @DisplayName("视频管理：CRUD/时长补录/节点整体替换/删除，引用归零文件 status=0")
    void videoAdminCrudAndDeleteConsistency() {
        StorageFile videoFile = upload(minimalMp4(120), "测试视频.mp4");
        Long videoId = videoAdminService.create(new VideoSaveRequest("测试视频", "介绍", null,
                videoFile.getId(), null, 1));
        assertEquals(120, videoService.detail(videoId).duration(), "时长应取 storage_file 探测值");

        // 节点保存 + 整体替换（空列表清空）
        videoAdminService.saveChapters(videoId, new VideoChapterBatchRequest(List.of(
                new VideoChapterBatchRequest.Item("节点1", 0, 0),
                new VideoChapterBatchRequest.Item("节点2", 60, 1))));
        assertEquals(2, videoService.detail(videoId).chapters().size());
        videoAdminService.saveChapters(videoId, new VideoChapterBatchRequest(List.of()));
        assertTrue(videoService.detail(videoId).chapters().isEmpty(), "空列表应清空全部节点");

        // 时长手工补录（探测失败场景）
        videoAdminService.update(videoId, new VideoSaveRequest("测试视频", "介绍", null,
                videoFile.getId(), 999, 1));
        assertEquals(999, videoService.detail(videoId).duration(), "手工补录时长应覆盖探测值");

        // 删除：视频文件引用归零 → status=0，节点清理
        videoAdminService.delete(videoId);
        assertEquals(0, storageFileRepository.findById(videoFile.getId()).orElseThrow().getStatus());
        assertTrue(storageRefRepository.findByBizTypeAndBizId("video", videoId).isEmpty());
    }

    // ===== 工具方法 =====

    /**
     * 分片上传-合并（单分片小文件场景；同内容已存在时秒传直接返回现有文件）
     */
    private StorageFile upload(byte[] content, String name) {
        String md5 = DigestUtil.sha256Hex(new ByteArrayInputStream(content));
        StorageInitVO init = fileUploadService.init(
                new StorageInitRequest(md5, (long) content.length, name, 1));
        if (init.uploadId() == null) {
            // 秒传命中：返回已入库文件
            return storageFileRepository.findById(init.fileId()).orElseThrow();
        }
        MultipartFile file = new MockMultipartFile("file", "chunk-0", "application/octet-stream", content);
        fileUploadService.chunk(init.uploadId(), 0, file);
        StorageMergeVO merge = fileUploadService.merge(init.uploadId());
        return storageFileRepository.findById(merge.fileId()).orElseThrow();
    }

    /**
     * 取 seed 图集的一张图片文件（签名访问测试数据源；fileId 从签名 URL 解析）
     */
    private StorageFile seedImageFile() {
        Long albumId = albumService.pageList(1, 10, null).records().get(0).id();
        String url = albumService.detail(albumId).images().get(0).url();
        Long fileId = Long.parseLong(url.replaceAll(".*/storage/file/(\\d+).*", "$1"));
        return storageFileRepository.findById(fileId).orElseThrow();
    }

    /**
     * 构造带 GPS EXIF 的 JPEG（Commons Imaging 写入，模拟手机照片定位信息）
     */
    private byte[] jpegWithGps() throws Exception {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(180, 154, 110));
        graphics.fillRect(0, 0, 64, 64);
        graphics.dispose();
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", raw);

        TiffOutputSet outputSet = new TiffOutputSet();
        var gps = outputSet.getOrCreateGPSDirectory();
        gps.add(GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF, "N");
        gps.add(GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF, "E");
        gps.add(GpsTagConstants.GPS_TAG_GPS_LATITUDE,
                new RationalNumber(31, 1), new RationalNumber(0, 1), new RationalNumber(0, 1));
        gps.add(GpsTagConstants.GPS_TAG_GPS_LONGITUDE,
                new RationalNumber(108, 1), new RationalNumber(0, 1), new RationalNumber(0, 1));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new ExifRewriter().updateExifMetadataLossless(new ByteArrayInputStream(raw.toByteArray()), out, outputSet);
        return out.toByteArray();
    }

    /**
     * 字节流中是否含 JPEG APP1 段标记（FF E1，EXIF 载体）
     */
    private static boolean containsApp1(byte[] bytes) {
        for (int i = 0; i < bytes.length - 1; i++) {
            if ((bytes[i] & 0xFF) == 0xFF && (bytes[i + 1] & 0xFF) == 0xE1) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构造最小 MP4（ftyp + moov/mvhd 无音视频轨，136 字节；时长可被内置解析读出）
     */
    private static byte[] minimalMp4(int durationSeconds) {
        ByteBuffer buf = ByteBuffer.allocate(136);
        buf.putInt(20);
        buf.put("ftyp".getBytes(StandardCharsets.US_ASCII));
        buf.put("isom".getBytes(StandardCharsets.US_ASCII));
        buf.putInt(0x200);
        buf.put("isom".getBytes(StandardCharsets.US_ASCII));
        buf.putInt(116);
        buf.put("moov".getBytes(StandardCharsets.US_ASCII));
        buf.putInt(108);
        buf.put("mvhd".getBytes(StandardCharsets.US_ASCII));
        buf.put((byte) 0);
        buf.put(new byte[]{0, 0, 0});
        buf.putInt(0);
        buf.putInt(0);
        buf.putInt(1000);
        buf.putInt(durationSeconds * 1000);
        buf.putInt(0x00010000);
        buf.putShort((short) 0x0100);
        buf.putShort((short) 0);
        buf.put(new byte[8]);
        buf.putInt(0x00010000);
        buf.putInt(0);
        buf.putInt(0);
        buf.putInt(0);
        buf.putInt(0x00010000);
        buf.putInt(0);
        buf.putInt(0);
        buf.putInt(0);
        buf.putInt(0x40000000);
        buf.put(new byte[24]);
        buf.putInt(2);
        return buf.array();
    }

    /**
     * 渲染渐变 PNG（seed 同款，作为测试图片内容）
     */
    private static byte[] renderPng(int fromRgb) {
        int width = 32;
        int height = 24;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int fromR = (fromRgb >> 16) & 0xFF;
        int fromG = (fromRgb >> 8) & 0xFF;
        int fromB = fromRgb & 0xFF;
        for (int y = 0; y < height; y++) {
            int rgb = (fromR << 16) | (fromG << 8) | fromB;
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, rgb);
            }
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("测试图片渲染失败", e);
        }
    }
}
