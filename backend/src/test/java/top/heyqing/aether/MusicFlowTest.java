package top.heyqing.aether;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

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

import top.heyqing.aether.exception.BusinessException;
import top.heyqing.aether.model.dto.MusicAlbumSaveRequest;
import top.heyqing.aether.model.dto.MusicSaveRequest;
import top.heyqing.aether.model.dto.StorageInitRequest;
import top.heyqing.aether.model.entity.Music;
import top.heyqing.aether.model.entity.StorageFile;
import top.heyqing.aether.model.vo.StorageInitVO;
import top.heyqing.aether.model.vo.StorageMergeVO;
import top.heyqing.aether.repository.MusicRepository;
import top.heyqing.aether.repository.StorageFileRepository;
import top.heyqing.aether.repository.StorageRefRepository;
import top.heyqing.aether.service.music.MusicAdminService;
import top.heyqing.aether.service.music.MusicService;
import top.heyqing.aether.service.storage.FileUploadService;
import top.heyqing.aether.util.DigestUtil;
import top.heyqing.aether.util.EffectConfigValidator;

/**
 * 音乐模块功能测试（阶段 4 完成标准，BackEnd-Plan §5.2/§7.3/§8.2/§8.3）
 *
 * <p>测试自建数据（DataSeeder 无音乐种子，各用例结束时清理，避免跨用例总量漂移）。
 * 覆盖：公开合集列表/类型过滤/详情曲目/单曲搜索/详情（歌词+特效配置+时长）、
 * 特效生成（调色板提取 + Schema 校验 + 粒子/波形/节拍三类绑定齐全）、
 * 手工调参配置校验拒绝（幻觉字段/版本错误）、MP3 内置时长解析回退（本机无 ffprobe）、
 * 管理 CRUD 与引用归零删除一致性。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = "aether.storage.local-base-dir=target/test-storage")
class MusicFlowTest {

    /** 合法 EffectConfig 样例（flowline 层，覆盖校验通过路径） */
    private static final String VALID_CONFIG = "{\"version\":1,\"palette\":[\"#5B8DEF\"],"
            + "\"layers\":[{\"type\":\"flowline\",\"bind\":\"amplitude\",\"count\":60,"
            + "\"speed\":0.8,\"opacity\":0.5}]}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MusicService musicService;

    @Autowired
    private MusicAdminService musicAdminService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private MusicRepository musicRepository;

    @Autowired
    private StorageFileRepository storageFileRepository;

    @Autowired
    private StorageRefRepository storageRefRepository;

    // ===== 公开接口（自建数据） =====

    @Test
    @DisplayName("合集公开接口：分页列表/类型过滤/详情曲目/推荐（封面签名 URL + trackCount）")
    void albumsPublicApi() throws Exception {
        StorageFile cover = upload(renderPng(0x3E5C76), "音乐封面.png");
        StorageFile audio = upload(minimalMp3(100), "合集曲目.mp3");
        // 先建无封面固定合集、后建有封面自定义合集：列表 id 降序 → records[0] 为带封面合集
        Long certifiedId = musicAdminService.albumCreate(
                new MusicAlbumSaveRequest("认证专辑测试", null, null, 2, "发行方：测试 · 编号 T-1", 1));
        Long customId = musicAdminService.albumCreate(
                new MusicAlbumSaveRequest("黄昏电台测试", cover.getId(), "介绍", 1, null, 1));
        Long trackId = musicAdminService.create(new MusicSaveRequest(
                "测试曲目", "测试歌手", customId, null, audio.getId(), null, 0, null, null, null, 0));
        try {
            mockMvc.perform(get("/v1/music/albums"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.records[0].coverUrl")
                            .value(org.hamcrest.Matchers.containsString("sign=")))
                    .andExpect(jsonPath("$.data.records[0].trackCount")
                            .value(org.hamcrest.Matchers.greaterThan(0)));

            // 类型过滤：固定合集（type=2）记录必须带认证信息
            mockMvc.perform(get("/v1/music/albums").param("type", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.records[0].certification").isNotEmpty());

            // 详情：曲目列表齐全且时长为探测值（100 帧 → 3s）
            mockMvc.perform(get("/v1/music/albums/{id}", customId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.type").value(1))
                    .andExpect(jsonPath("$.data.tracks.length()").value(1))
                    .andExpect(jsonPath("$.data.tracks[0].duration").value(3))
                    .andExpect(jsonPath("$.data.tracks[0].title").value("测试曲目"));

            mockMvc.perform(get("/v1/music/recommend"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.length()").value(2));
        } finally {
            musicAdminService.delete(trackId);
            musicAdminService.albumDelete(certifiedId);
            musicAdminService.albumDelete(customId);
        }
    }

    @Test
    @DisplayName("单曲公开接口：搜索（歌名）/详情（LRC 歌词 + EffectConfig + 偏移负值）")
    void musicPublicApi() throws Exception {
        StorageFile audio = upload(minimalMp3(100), "单曲.mp3");
        String lyric = "[ti:测试曲]\n[00:00.00]测试曲\n[00:12.50]歌词第二行";
        Long trackId = musicAdminService.create(new MusicSaveRequest(
                "无人灯塔测试曲", "测试歌手", null, null, audio.getId(), lyric, -500, null,
                VALID_CONFIG, 2, 0));
        try {
            // keyword 搜索命中歌名
            mockMvc.perform(get("/v1/music").param("keyword", "无人灯塔测试曲"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.total").value(1));

            // 详情：LRC 歌词 + 特效配置 + 负偏移 + 音频签名 URL
            mockMvc.perform(get("/v1/music/{id}", trackId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.fileUrl")
                            .value(org.hamcrest.Matchers.containsString("sign=")))
                    .andExpect(jsonPath("$.data.lyricText")
                            .value(org.hamcrest.Matchers.containsString("[00:12.50]")))
                    .andExpect(jsonPath("$.data.lyricOffset").value(-500))
                    .andExpect(jsonPath("$.data.effectConfig")
                            .value(org.hamcrest.Matchers.containsString("flowline")))
                    .andExpect(jsonPath("$.data.effectSource").value(2));

            // 不存在的曲目 → 30301
            mockMvc.perform(get("/v1/music/{id}", 999999L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(30301));
        } finally {
            musicAdminService.delete(trackId);
        }
    }

    // ===== 特效生成与 Schema 校验（§7.3） =====

    @Test
    @DisplayName("特效生成：调色板提取 + 粒子/波形/节拍三类绑定齐全 + 校验后落库 source=1")
    void effectGenerationCoversAllBinds() {
        StorageFile audio = upload(minimalMp3(100), "特效曲目.mp3");
        Long trackId = musicAdminService.create(new MusicSaveRequest(
                "特效测试曲", null, null, null, audio.getId(), null, 0, null, null, null, 0));
        try {
            String config = musicAdminService.generateEffect(trackId);
            assertTrue(config.contains("\"particles\"") && config.contains("\"amplitude\""),
                    "粒子层（响度绑定）应存在");
            assertTrue(config.contains("\"wave\"") && config.contains("\"freqBand\""),
                    "波形层（频段绑定）应存在");
            assertTrue(config.contains("\"ring\"") && config.contains("\"beat\""),
                    "圆环层（节拍绑定）应存在");
            assertTrue(config.contains("\"palette\":[\"#"), "调色板应为十六进制色数组");
            assertNull(EffectConfigValidator.validate(config), "生成配置应通过 Schema 校验");

            Music reloaded = musicRepository.findById(trackId).orElseThrow();
            // JSON 列存储会规范化空白，按解析后的 JSON 树比较内容
            try {
                assertEquals(new com.fasterxml.jackson.databind.ObjectMapper().readTree(config),
                        new com.fasterxml.jackson.databind.ObjectMapper().readTree(reloaded.getEffectConfig()),
                        "生成配置应落库（JSON 内容一致）");
            } catch (Exception e) {
                throw new IllegalStateException("JSON 解析失败", e);
            }
            assertEquals(1, reloaded.getEffectSource(), "生成来源应标记为 1（生成）");
        } finally {
            musicAdminService.delete(trackId);
        }
    }

    @Test
    @DisplayName("手工调参：非法 Schema（幻觉字段/版本错误）拒绝落库，合法配置落库 source=2")
    void effectManualValidation() {
        StorageFile audio = upload(minimalMp3(100), "调参曲目.mp3");
        Long trackId = musicAdminService.create(new MusicSaveRequest(
                "调参测试曲", null, null, null, audio.getId(), null, 0, null, null, null, 0));
        try {
            Music track = musicRepository.findById(trackId).orElseThrow();

            // 幻觉字段拒绝
            BusinessException hallucinated = assertThrows(BusinessException.class, () ->
                    musicAdminService.update(trackId, new MusicSaveRequest(track.getTitle(), track.getArtist(),
                            track.getAlbumId(), track.getCoverFileId(), track.getFileId(), track.getLyricText(),
                            track.getLyricOffset(), track.getDuration(),
                            "{\"version\":1,\"palette\":[\"#FFFFFF\"],\"layers\":[{\"type\":\"wave\","
                                    + "\"bind\":\"amplitude\",\"ghostField\":1}]", 2, 0)));
            assertEquals(10001, hallucinated.getErrorCode().getCode(), "幻觉字段应返回参数错误");

            // 版本错误拒绝
            BusinessException badVersion = assertThrows(BusinessException.class, () ->
                    musicAdminService.update(trackId, new MusicSaveRequest(track.getTitle(), track.getArtist(),
                            track.getAlbumId(), track.getCoverFileId(), track.getFileId(), track.getLyricText(),
                            track.getLyricOffset(), track.getDuration(),
                            "{\"version\":9,\"palette\":[\"#FFFFFF\"],\"layers\":[{\"type\":\"wave\","
                                    + "\"bind\":\"amplitude\"}]", 2, 0)));
            assertEquals(10001, badVersion.getErrorCode().getCode());

            // 合法配置落库 + 来源标记手工（2）
            musicAdminService.update(trackId, new MusicSaveRequest(track.getTitle(), track.getArtist(),
                    track.getAlbumId(), track.getCoverFileId(), track.getFileId(), track.getLyricText(),
                    track.getLyricOffset(), track.getDuration(), VALID_CONFIG, 2, 0));
            Music updated = musicRepository.findById(trackId).orElseThrow();
            assertEquals(VALID_CONFIG, updated.getEffectConfig());
            assertEquals(2, updated.getEffectSource());
        } finally {
            musicAdminService.delete(trackId);
        }
    }

    // ===== MP3 时长探测回退（§8.2） =====

    @Test
    @DisplayName("MP3 时长探测回退：无 ffprobe 时内置帧结构解析出时长（CBR 估算）")
    void mp3DurationProbedWithoutFfprobe() {
        // 200 帧 × 417 字节 = 83400 字节，128kbps → 5.21s（ffprobe 存在时同为 5s，两路结果一致）
        byte[] mp3 = minimalMp3(200);
        StorageFile file = upload(mp3, "测试音频.mp3");
        assertEquals(5, file.getDuration(), "本机无 ffprobe，应走内置 MP3 帧解析回退");
        assertEquals("mp3", file.getExt());
    }

    // ===== 管理 CRUD + 删除一致性（§8.3） =====

    @Test
    @DisplayName("音乐管理：合集/单曲 CRUD、固定合集认证必填、删集防呆、引用归零清理")
    void musicAdminCrudAndDeleteConsistency() {
        // 固定合集必须认证
        BusinessException noCert = assertThrows(BusinessException.class, () ->
                musicAdminService.albumCreate(new MusicAlbumSaveRequest("无认证专辑", null, null, 2, null, 0)));
        assertEquals(10001, noCert.getErrorCode().getCode());

        StorageFile cover = upload(renderPng(0x4F6D5A), "管理封面.png");
        StorageFile audio = upload(minimalMp3(100), "管理曲目.mp3");

        Long albumId = musicAdminService.albumCreate(
                new MusicAlbumSaveRequest("管理测试合集", cover.getId(), "介绍", 1, null, 0));
        Long trackId = musicAdminService.create(new MusicSaveRequest(
                "管理测试曲目", "测试歌手", albumId, null, audio.getId(), null, 0, null, null, null, 1));
        // 时长应取 storage_file 探测值（100 帧 → 3s）
        assertEquals(3, musicService.detail(trackId).duration());
        assertEquals(1, musicService.albumDetail(albumId).tracks().size());

        // 含曲目删集被拒
        BusinessException blocked = assertThrows(BusinessException.class,
                () -> musicAdminService.albumDelete(albumId));
        assertEquals(10001, blocked.getErrorCode().getCode());

        // 换音频文件：旧文件引用归零 → status=0
        StorageFile audio2 = upload(minimalMp3(120), "管理曲目2.mp3");
        musicAdminService.update(trackId, new MusicSaveRequest(
                "管理测试曲目", "测试歌手", albumId, null, audio2.getId(), null, 0, null, null, null, 1));
        assertEquals(0, storageFileRepository.findById(audio.getId()).orElseThrow().getStatus(),
                "旧音频引用归零应标记待清理");

        // 删曲目 → 新文件归零 + 引用清空
        musicAdminService.delete(trackId);
        assertEquals(0, storageFileRepository.findById(audio2.getId()).orElseThrow().getStatus());
        assertTrue(storageRefRepository.findByBizTypeAndBizId("music", trackId).isEmpty(),
                "删除曲目后 storage_ref 应无残留引用");

        // 删合集（已无曲目）→ 封面归零 + 引用清空
        musicAdminService.albumDelete(albumId);
        assertEquals(0, storageFileRepository.findById(cover.getId()).orElseThrow().getStatus());
        assertTrue(storageRefRepository.findByBizTypeAndBizId("music", albumId).isEmpty(),
                "删除合集后 storage_ref 应无残留引用");
    }

    // ===== 工具方法 =====

    /**
     * 分片上传-合并（单分片小文件场景；同内容已存在时秒传直接返回现有文件）
     */
    private StorageFile upload(byte[] content, String name) {
        String md5 = DigestUtil.sha256Hex(new java.io.ByteArrayInputStream(content));
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
     * 构造最小 MP3（MPEG1 Layer III 128kbps/44.1kHz 立体声帧，帧长 417 字节；
     * 无 Xing/VBRI 头 → 内置解析走 CBR 码率估算路径）
     */
    private static byte[] minimalMp3(int frames) {
        int frameSize = 417;
        byte[] data = new byte[frames * frameSize];
        for (int i = 0; i < frames; i++) {
            int offset = i * frameSize;
            data[offset] = (byte) 0xFF;
            data[offset + 1] = (byte) 0xFB; // MPEG1 Layer III 无 CRC
            data[offset + 2] = (byte) 0x90; // 码率索引 9(128k) + 44.1kHz + 无 padding
            data[offset + 3] = 0x00;        // 立体声
            for (int j = 4; j < frameSize; j++) {
                data[offset + j] = (byte) 0x55;
            }
        }
        return data;
    }

    /**
     * 渲染纯色 PNG（作为测试封面，供调色板提取）
     */
    private static byte[] renderPng(int rgb) {
        var image = new java.awt.image.BufferedImage(32, 24, java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 24; y++) {
            for (int x = 0; x < 32; x++) {
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
