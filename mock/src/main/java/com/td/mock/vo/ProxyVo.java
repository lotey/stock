/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.mock.vo;

import lombok.Data;

/**
 * @auther lotey
 * @date 2020/2/8 19:41
 * @desc 代理IP实体类
 */
@Data
public class ProxyVo {

    private String ip;

    private Integer port;

    private String type;
}
