package top.heyqing.aether.storage;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.exception.BusinessException;

/**
 * 存储后端路由器：按 storageType（1 本地 / 2 OSS）选择实现（BackEnd-Plan §8.1）
 *
 * <p>未找到对应实现（如 OSS 凭据未配置未装配）时给出明确业务错误。</p>
 */
@Component
public class StorageRouter {

    private final Map<Integer, StorageService> byType;

    public StorageRouter(List<StorageService> storageServices) {
        this.byType = storageServices.stream()
                .collect(Collectors.toMap(StorageService::storageType, Function.identity()));
    }

    /**
     * 选择存储实现
     *
     * @param storageType 1 本地 / 2 OSS
     * @throws BusinessException 50002（对应后端未装配，如 OSS 未配置）
     */
    public StorageService select(int storageType) {
        StorageService service = byType.get(storageType);
        if (service == null) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR,
                    storageType == 2 ? "OSS 未配置（OSS_ENDPOINT/OSS_ACCESS_KEY/OSS_SECRET/OSS_BUCKET）" : "存储后端不存在");
        }
        return service;
    }
}
