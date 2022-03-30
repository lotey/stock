/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @auther lotey
 * @date 8/15/20 4:25 PM
 * @desc 功能描述
 */
@Data
public class LastNPriceVo {

    private String code;

    private BigDecimal maxPrice;

    private BigDecimal minPrice;
}
