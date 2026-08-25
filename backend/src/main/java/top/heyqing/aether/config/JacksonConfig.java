package top.heyqing.aether.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * Jackson 全局配置（BackEnd-Plan §2 关键约定：时间字段对外统一 yyyy-MM-dd HH:mm:ss）
 *
 * <p>Boot 4.1 使用 Jackson 3（tools.jackson 包），经 JsonMapperBuilderCustomizer
 * 注册 LocalDateTime 序列化/反序列化格式。</p>
 */
@Configuration
public class JacksonConfig {

    /** 对外时间格式（BackEnd-Plan §2） */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public JsonMapperBuilderCustomizer aetherJsonCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule("aether-time");
            module.addSerializer(LocalDateTime.class, new ValueSerializer<>() {
                @Override
                public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext context)
                        throws JacksonException {
                    gen.writeString(value.format(DATE_TIME_FORMATTER));
                }
            });
            module.addDeserializer(LocalDateTime.class, new ValueDeserializer<>() {
                @Override
                public LocalDateTime deserialize(JsonParser parser, DeserializationContext context)
                        throws JacksonException {
                    return LocalDateTime.parse(parser.getString(), DATE_TIME_FORMATTER);
                }
            });
            builder.addModule(module);
        };
    }
}
