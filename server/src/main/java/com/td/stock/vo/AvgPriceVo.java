/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 均价实体类
 */
@Data
public class AvgPriceVo {

    private BigDecimal avg;

    private BigDecimal avg5;

    private BigDecimal avg10;

    private BigDecimal avg20;

    private BigDecimal avg30;

    private Integer last10Trend;

    private Integer last10MonthTrend;

    private BigDecimal avgMin;

    private BigDecimal avgMax;
}
