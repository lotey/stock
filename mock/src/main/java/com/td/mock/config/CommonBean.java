/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.mock.config;

import com.td.common.common.MybatisBatchHandler;
import com.td.common.util.HttpClientUtil;
import com.td.common.util.SnowflakeGenIdUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @auther lotey
 * @date 2020/1/24 14:50
 * @desc 外部bean实例化
 */
@Component
public class CommonBean {

    @Bean("clientUtil")
    public HttpClientUtil getClientUtilBean() {
        HttpClientUtil clientUtil = new HttpClientUtil();
        clientUtil.setMaxRetryCount(3);
        return clientUtil;
    }

    @Bean("genIdUtil")
    public SnowflakeGenIdUtil getGenIdUtilBean() {
        return new SnowflakeGenIdUtil(1,1);
    }

    @Bean("mybatisBatchHandler")
    public MybatisBatchHandler getMybatisBatchHandlerBean() {
        return new MybatisBatchHandler();
    }
}
