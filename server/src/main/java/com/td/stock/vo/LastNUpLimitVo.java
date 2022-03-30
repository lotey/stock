/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.vo;

import lombok.Data;

/***
 * 最近N天的涨停实体
 */
@Data
public class LastNUpLimitVo {

    private String code;

    private Integer count;
}
