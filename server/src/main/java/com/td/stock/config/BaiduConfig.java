/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @auther lotey
 * @date 2020/1/6 22:02
 * @desc 百度配置文件类
 */
@Component
@ConfigurationProperties(prefix="baidu.sdk")
@Data
public class BaiduConfig {

    private String apkKey;

    private String secretKey;
}
