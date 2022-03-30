/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.vo;

import lombok.Data;

/**
 * @auther lotey
 * @date 2021/3/31 21:16
 * @desc 涨停跌停和连板数
 */
@Data
public class CodeLimitUDPVo {

    private String code;

    private Double upLimitVal;

    private Double downLimitVal;

    private String pDesc;
}
