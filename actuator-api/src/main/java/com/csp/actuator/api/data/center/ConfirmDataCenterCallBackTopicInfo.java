package com.csp.actuator.api.data.center;

import com.csp.actuator.api.base.DataCenterInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 确认数据中心回调Topic信息
 *
 * @author Weijia Jiang
 * @version v1
 * @description 确认数据中心回调Topic信息
 * @date Created in 2023-10-08 15:49
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConfirmDataCenterCallBackTopicInfo extends DataCenterInfo {
    private Integer status;
}
