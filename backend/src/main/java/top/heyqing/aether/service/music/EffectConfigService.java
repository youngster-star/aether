package top.heyqing.aether.service.music;

/**
 * EffectConfig 生成服务（BackEnd-Plan §7.3）
 *
 * <p>阶段 4 先行：封面调色板提取（k-means 主色）+ 确定性规则生成 + Schema 校验，
 * 覆盖粒子/波形/节拍三类绑定；LLM 结构化生成在阶段 7 LangChain4j 接入后切换
 * （接入点即 generate 内部实现，接口与校验链路不变）。</p>
 */
public interface EffectConfigService {

    /**
     * 生成 EffectConfig JSON（必须通过 Schema v1 校验后才可落库）
     *
     * @param coverFileId 封面文件 ID（调色板提取源，可空=默认调色板）
     * @param duration    曲目时长（秒，用于节奏参数估计）
     * @return 合法配置 JSON 串
     */
    String generate(Long coverFileId, Integer duration);
}
