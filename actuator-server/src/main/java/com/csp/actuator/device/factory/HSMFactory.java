package com.csp.actuator.device.factory;


import com.csp.actuator.api.entity.GenerateKeyResult;
import com.csp.actuator.api.entity.RemoveKeyInfo;
import com.csp.actuator.device.contants.GlobalAlgTypeCodeConstant;
import com.csp.actuator.device.contants.GlobalTypeCodeConstant;
import com.csp.actuator.device.contants.GlobalUsedTypeCodeConstant;

import java.util.List;

/**
 * HSM抽象工厂接口
 *
 * @author Weijia Jiang
 * @version v1
 * @description HSM抽象工厂接口
 * @date Created in 2023-03-17 11:45
 */
public interface HSMFactory {

    /**
     * 删除密钥
     *
     * @param deviceInfoPort    设备信息
     * @param removeKeyInfoList 需要移除的密钥信息
     * @return 是否成功
     */
    Boolean removeKey(List<String> deviceInfoPort, List<RemoveKeyInfo> removeKeyInfoList);

    /**
     * 根据密钥类型获取指定索引信息
     *
     * @param globalKeyType  密钥类型:{@link GlobalTypeCodeConstant}
     * @param globalKeyUsage 密钥用途:{@link GlobalUsedTypeCodeConstant}
     * @param startKeyIndex  起始索引
     * @param devicePostList 操作设备列表
     * @return 所有的索引列表
     */
    List<Integer> listKeys(int globalKeyType, int globalKeyUsage, int startKeyIndex, List<String> devicePostList);


    /**
     * 生成对称密钥
     *
     * @param keyAlgType     密钥算法类型：{@link GlobalAlgTypeCodeConstant}
     * @param devicePostList 操作的设备列表
     * @return 结果集
     */
    GenerateKeyResult generateSymmetricKey(int keyAlgType, List<String> devicePostList);

    /**
     * 生成并保存对称密钥
     *
     * @param keyAlgType     密钥算法类型：{@link GlobalAlgTypeCodeConstant}
     * @param keyIndex       保存的密钥索引位置
     * @param devicePostList 操作的设备列表
     * @return 结果集
     */
    GenerateKeyResult generateAndSaveSymmetricKey(int keyAlgType, Integer keyIndex, List<String> devicePostList);

    GenerateKeyResult generateSymmetricKey4ProKeyIndex(Integer globalKeyType, Integer proKeyIndex, Integer proKeyGlobalKeyType, String destKeyIV, int encDerivedAlg, List<String> devicePostList);

    GenerateKeyResult generateSymmetricKey4ProKeyInfo(Integer globalKeyType, String proKeyInfo, Integer proKeyGlobalKeyType, String proKeyCv, String destKeyIV, int encDerivedAlg, List<String> devicePostList);

    String generateRandom(List<String> devicePostList, int length);

    GenerateKeyResult generateSM2Key(Integer proKekIndex, List<String> devicePostList);

    GenerateKeyResult generateSM2Key4ProKeyValue(Integer globalAlgTypeCode, String proKekInfo, List<String> devicePostList);

    Boolean importSymmetricKey(int keyIndex, Integer globalAlgTypeCode, String cipherByLMK, String keyCV, List<String> devicePostList);

    Boolean importSymmetricKey(int kekIndex, int keyIndex, Integer globalAlgTypeCode, String cipherByLMK, String keyCV, List<String> devicePostList);

    void importSM2Key(Integer kekIndex, Integer globalKeyType, String cipherByLMK, int keyIndex, int keyUsedType, String keyLable, String keyId, List<String> devicePostList);

    GenerateKeyResult generateAndSaveSM2Key(Integer kekIndex, Integer globalKeyType, int keyIndex, Integer keyUsedType, String keyLable, List<String> devicePostList);

    GenerateKeyResult generateAndSaveSymmetricKey4ProKeyIndex(Integer globalKeyType, Integer proKeyIndex, Integer proKeyGlobalKeyType, Integer keyIndex,
                                                              String keyLabel, String destKeyIV, int encDerivedAlg, List<String> devicePostList);

}
