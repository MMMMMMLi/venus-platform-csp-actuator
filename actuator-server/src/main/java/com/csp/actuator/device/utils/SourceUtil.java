package com.csp.actuator.device.utils;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.stream.Stream;


/**
 * 文件工具类
 *
 * @author Weijia Jiang
 * @version v1
 * @description 文件工具类
 * @date Created in 2023-04-25 17:46
 */
@Slf4j
public class SourceUtil {

    public static final String signCertPath = System.getProperty("user.dir") + File.separator + "sm2_client.pfx";
    public static final String encCertPath = System.getProperty("user.dir") + File.separator + "sm2_client_enc.pfx";
    public static final String rootCertPath = System.getProperty("user.dir") + File.separator + "sm2_root.cer";
    public static final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
    private static final String[] configLocations = new String[]{"classpath*:/cert/*.*"};

    public static boolean buildCertFileToLocal() {
        try {
            Resource[] resources = resolveConfigLocations();
            log.info("获取到cert目录下文件数:{}个", resources.length);
            for (Resource rs : resources) {
                InputStream inputStream = rs.getInputStream();
                String filename = rs.getFilename();
                if (StringUtils.isBlank(filename)) {
                    continue;
                }
                //将文件写入临时目录
                File tempFile = new File(filename);
                FileUtils.copyInputStreamToFile(inputStream, tempFile);
            }
        } catch (IOException e) {
            log.error("加载证书路径path异常!", e);
            return false;
        }
        return true;
    }

    public static void removeLocalCertFile() {
        File signCertFile = new File(signCertPath);
        File encCertFile = new File(encCertPath);
        File rootCertFile = new File(rootCertPath);
        if (signCertFile.exists()) {
            if (signCertFile.delete()) {
                log.info("本地签名证书删除成功!");
            }
        }
        if (encCertFile.exists()) {
            if (encCertFile.delete()) {
                log.info("本地加密证书删除成功!");
            }
        }
        if (rootCertFile.exists()) {
            if (rootCertFile.delete()) {
                log.info("本地根证书删除成功!");
            }
        }
    }

    public static Resource[] resolveConfigLocations() {
        return Stream.of(Optional.of(configLocations).orElse(new String[0]))
                .flatMap(location -> Stream.of(getResources(location))).toArray(Resource[]::new);
    }

    public static Resource[] getResources(String location) {
        try {
            return resourceResolver.getResources(location);
        } catch (IOException e) {
            log.error("获取资源文件出错!location:{}", location, e);
            return new Resource[0];
        }
    }
}
