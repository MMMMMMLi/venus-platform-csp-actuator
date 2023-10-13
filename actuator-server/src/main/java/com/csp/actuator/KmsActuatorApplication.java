package com.csp.actuator;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.csp.actuator.api.enums.ActuatorStatusEnum;
import com.csp.actuator.cache.DataCenterKeyCache;
import com.csp.actuator.report.NodeReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

@Slf4j
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class KmsActuatorApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext application = SpringApplication.run(KmsActuatorApplication.class, args);
        Environment environment = application.getEnvironment();
        // 检查属性是否存在
        String dataCenterId = environment.getProperty("data.center.id");
        if (NodeReport.dataCenterInfoIsError(dataCenterId)) {
            log.error("Custom property 'data.center.id' not setting. Stopping application.");
            System.exit(1);
        } else {
            log.info("Custom property 'data.center.id' settings completed.");
        }
        String dataCenterName = environment.getProperty("data.center.name");
        if (NodeReport.dataCenterInfoIsError(dataCenterName)) {
            log.error("Custom property 'data.center.name' not setting. Stopping application.");
            System.exit(1);
        } else {
            log.info("Custom property 'data.center.name' settings completed.");
        }
        // 校验kms地址
        String kmsAddress = environment.getProperty("kms.address");
        if (NodeReport.dataCenterInfoIsError(kmsAddress)) {
            log.error("Custom property 'kms.address' not setting. Stopping application.");
            System.exit(1);
        } else {
            log.info("Custom property 'kms.address' settings completed.");
        }
        String kmsSecret = environment.getProperty("kms.secret");
        if (NodeReport.dataCenterInfoIsError(kmsSecret)) {
            log.error("Custom property 'kms.secret' not setting. Stopping application.");
            System.exit(1);
        } else {
            log.info("Custom property 'kms.secret' settings completed.");
        }
//        // 获取密钥
//        if (!DataCenterKeyCache.initDataCenterKey(kmsAddress, kmsSecret)) {
//            log.error("InitDataCenterKey failed. Stopping application.");
//            System.exit(1);
//        } else {
//            log.info("DataCenterKey settings completed.");
//        }
        log.info("\n-------------------------------------------------------------------------\n\t" +
                "Actuator Server Successfully started ...\n\t" +
                "DataCenterId :\t" + dataCenterId + "\n\t" +
                "DataCenterName :\t" + dataCenterName + "\n" +
                "-------------------------------------------------------------------------");
        // 启动成功之后，开始上报节点信息
        log.info("执行节点启动成功，开始上报节点信息...");
        NodeReport.producerMessage(
                NodeReport.getActuatorNodeStatusTopicInfo(
                        dataCenterId, dataCenterName, environment.getProperty("server.port"), ActuatorStatusEnum.INIT));
    }

}
