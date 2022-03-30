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
 * 钉钉的配置类
 */
@Component
@ConfigurationProperties(prefix="dingding")
@Data
public class DingdingConfig {

    private String webHook;
}
