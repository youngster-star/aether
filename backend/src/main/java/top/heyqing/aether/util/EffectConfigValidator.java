package top.heyqing.aether.util;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * EffectConfig JSON Schema 校验（BackEnd-Plan §7.3 Schema v1）
 *
 * <p>AI 生成与人工调参的配置落库前必须通过本校验（防 AI 幻觉字段）：
 * 版本、调色板、图层类型/绑定枚举、逐类型字段范围、背景/过渡/沙箱代码均严格校验，
 * 未知字段一律拒绝。纯静态工具，无外部 schema 依赖。</p>
 */
public final class EffectConfigValidator {

    /** 图层类型枚举（§7.3 layers[].type） */
    private static final Set<String> LAYER_TYPES = Set.of("particles", "wave", "ring", "flowline", "text");

    /** 音频特征绑定枚举（§7.3 layers[].bind） */
    private static final Set<String> BINDS = Set.of("amplitude", "freqBand", "beat");

    /** 过渡缓动枚举 */
    private static final Set<String> EASINGS = Set.of("linear", "easeOutCubic", "easeInOutQuad");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 沙箱代码上限（32KB，防超大脚本注入存储） */
    private static final int SANDBOX_CODE_MAX_LENGTH = 32 * 1024;

    private EffectConfigValidator() {
    }

    /**
     * 校验 EffectConfig JSON（Schema v1）
     *
     * @param json 配置原始 JSON 串
     * @return 合法返回 null；非法返回原因说明（调用方决定错误码语义）
     */
    public static String validate(String json) {
        if (json == null || json.isBlank()) {
            return "配置不能为空";
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (Exception e) {
            return "配置不是合法 JSON";
        }
        if (!root.isObject()) {
            return "配置必须是 JSON 对象";
        }
        if (root.has("sandboxCode") && !root.get("sandboxCode").isNull()
                && (!root.get("sandboxCode").isTextual()
                        || root.get("sandboxCode").asText().length() > SANDBOX_CODE_MAX_LENGTH)) {
            return "sandboxCode 必须为空或不超过 32KB 的字符串";
        }
        if (root.size() > 8) {
            return "存在未知字段";
        }
        JsonNode version = root.get("version");
        if (version == null || !version.canConvertToInt() || version.asInt() != 1) {
            return "version 必须为 1";
        }
        String error = validatePalette(root.get("palette"));
        if (error != null) {
            return error;
        }
        error = validateLayers(root.get("layers"));
        if (error != null) {
            return error;
        }
        error = validateBackground(root.get("background"));
        if (error != null) {
            return error;
        }
        return validateTransition(root.get("transition"));
    }

    /**
     * 调色板：1-8 个 #RRGGBB 颜色
     */
    private static String validatePalette(JsonNode palette) {
        if (palette == null || !palette.isArray() || palette.isEmpty()
                || palette.size() > 8) {
            return "palette 必须为 1-8 个颜色的数组";
        }
        for (JsonNode color : palette) {
            if (!color.isTextual() || !isHexColor(color.asText())) {
                return "palette 含非法颜色值: " + color;
            }
        }
        return null;
    }

    /**
     * 图层：1-8 层，逐层按类型校验字段
     */
    private static String validateLayers(JsonNode layers) {
        if (layers == null || !layers.isArray() || layers.isEmpty() || layers.size() > 8) {
            return "layers 必须为 1-8 个图层的数组";
        }
        for (JsonNode layer : layers) {
            if (!layer.isObject()) {
                return "图层必须是对象";
            }
            String type = textOrNull(layer.get("type"));
            String bind = textOrNull(layer.get("bind"));
            if (type == null || !LAYER_TYPES.contains(type)) {
                return "图层 type 非法: " + type;
            }
            if (bind == null || !BINDS.contains(bind)) {
                return "图层 bind 非法: " + bind;
            }
            if (layer.size() > 8) {
                return "图层存在未知字段";
            }
            String error = switch (type) {
                case "particles" -> validateNumericField(layer, "count", 1, 500, true)
                        == null ? validateRangeField(layer, "sizeRange", 0.1, 50, true)
                        : validateNumericField(layer, "count", 1, 500, true);
                case "wave" -> validateRangeField(layer, "band", 0.0, 1.0, false);
                case "ring" -> null;
                case "flowline" -> validateNumericField(layer, "count", 1, 300, true);
                default -> validateTextField(layer, "content", 100);
            };
            if (error != null) {
                return error;
            }
            error = validateNumericField(layer, "amplitude", 0, 200, false);
            if (error != null) {
                return error;
            }
            error = validateNumericField(layer, "speed", 0, 5, false);
            if (error != null) {
                return error;
            }
            error = validateNumericField(layer, "opacity", 0, 1, false);
            if (error != null) {
                return error;
            }
            error = validateOptionalColor(layer, "color");
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    /**
     * 背景：可空；gradient 需 from+to，solid 需 from
     */
    private static String validateBackground(JsonNode background) {
        if (background == null || background.isNull()) {
            return null;
        }
        if (!background.isObject() || background.size() > 3) {
            return "background 字段非法";
        }
        String type = textOrNull(background.get("type"));
        if (!"gradient".equals(type) && !"solid".equals(type)) {
            return "background.type 仅支持 gradient/solid";
        }
        if (!background.has("from") || !isHexColor(background.get("from").asText(""))) {
            return "background.from 非法";
        }
        if ("gradient".equals(type)
                && (!background.has("to") || !isHexColor(background.get("to").asText("")))) {
            return "gradient 背景必须包含合法 to 颜色";
        }
        return null;
    }

    /**
     * 过渡：可空；duration 0-5000ms + 缓动枚举
     */
    private static String validateTransition(JsonNode transition) {
        if (transition == null || transition.isNull()) {
            return null;
        }
        if (!transition.isObject() || transition.size() > 2) {
            return "transition 字段非法";
        }
        JsonNode duration = transition.get("duration");
        if (duration == null || !duration.canConvertToInt()
                || duration.asInt() < 0 || duration.asInt() > 5000) {
            return "transition.duration 必须为 0-5000";
        }
        String easing = textOrNull(transition.get("easing"));
        if (easing != null && !EASINGS.contains(easing)) {
            return "transition.easing 非法: " + easing;
        }
        return null;
    }

    /**
     * 必填整数范围字段校验
     */
    private static String validateNumericField(JsonNode node, String field, int min, int max, boolean required) {
        JsonNode value = node.get(field);
        if (value == null) {
            return required ? "缺少必填字段: " + field : null;
        }
        if (!value.canConvertToInt() || value.asInt() < min || value.asInt() > max) {
            return field + " 必须为 " + min + "-" + max + " 的整数";
        }
        return null;
    }

    /**
     * 必填/可选二元范围数组校验（sizeRange/band：[min,max] 且 min<=max）
     */
    private static String validateRangeField(JsonNode node, String field, double min, double max, boolean required) {
        JsonNode value = node.get(field);
        if (value == null) {
            return required ? "缺少必填字段: " + field : null;
        }
        if (!value.isArray() || value.size() != 2
                || !value.get(0).isNumber() || !value.get(1).isNumber()) {
            return field + " 必须为二元数组";
        }
        double low = value.get(0).asDouble();
        double high = value.get(1).asDouble();
        if (low < min || high > max || low > high) {
            return field + " 必须在 " + min + "-" + max + " 且升序";
        }
        return null;
    }

    /**
     * text 图层 content 字段校验
     */
    private static String validateTextField(JsonNode node, String field, int maxLength) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()
                || value.asText().length() > maxLength) {
            return "text 图层 " + field + " 必须为 1-" + maxLength + " 字";
        }
        return null;
    }

    /**
     * 可选颜色字段校验
     */
    private static String validateOptionalColor(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value != null && (!value.isTextual() || !isHexColor(value.asText()))) {
            return field + " 非法颜色值: " + value.asText();
        }
        return null;
    }

    private static String textOrNull(JsonNode node) {
        return node == null || !node.isTextual() ? null : node.asText();
    }

    /**
     * #RRGGBB 颜色格式（大小写均可）
     */
    private static boolean isHexColor(String value) {
        return value != null && value.matches("^#[0-9a-fA-F]{6}$");
    }
}
