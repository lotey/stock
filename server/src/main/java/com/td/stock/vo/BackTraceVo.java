/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * @auther lotey
 * @date 6/13/20 4:41 PM
 * @desc 模型回测结果
 */
@Data
public class BackTraceVo {

    private String date;

    private String code;

    private String name;

    private BigDecimal closePrice;

    private BigDecimal nextDayMinPrice;

    private BigDecimal nextDayMaxPrice;

    private BigDecimal downRate;

    private BigDecimal winRate;

    private BigDecimal lostRate;

    private BigDecimal next3DayMaxWinRate;

    private BigDecimal next3DayMaxLostRate;
}
