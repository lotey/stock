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
 * @date 7/16/20 10:16 PM
 * @desc 功能描述
 */
@Component
@ConfigurationProperties(prefix="xiongmao")
@Data
public class XiongmaoConfig {

    private String secret;

    private String orderNo;
}
