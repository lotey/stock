/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.mock.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @auther lotey
 * @date 2020/1/4 22:20
 * @desc 代理配置
 */
@Component
@ConfigurationProperties(prefix="stock.proxy")
@Data
public class ProxyConfig {

    private String imgDownadDir;
}
