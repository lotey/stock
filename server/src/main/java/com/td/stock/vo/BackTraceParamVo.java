/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.vo;

import lombok.Data;

/**
 * @auther lotey
 * @date 8/8/20 9:55 PM
 * @desc 回溯入参实体
 */
@Data
public class BackTraceParamVo {

    private String code;

    private String name;

    private String date;
}
