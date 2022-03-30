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
 * @date 2020/1/4 22:20
 * @desc 代理配置
 */
@Component
@ConfigurationProperties(prefix="spider")
@Data
public class StockConfig {

    private String seleniumPath;

    private Integer count;

    private String tessdataDir;

    private String imgDownadDir;

    private String csvDownloadDir;

    private String backTraceLogDir;

    private Integer proxyType;
}
