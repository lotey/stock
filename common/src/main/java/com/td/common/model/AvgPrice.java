/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AvgPrice {

    private Long id;

    private String code;

    private String name;

    private LocalDate date;

    private BigDecimal avg;

    private BigDecimal avg5;

    private BigDecimal avg10;

    private BigDecimal avg20;

    private BigDecimal avg30;

    private Integer last10Trend;

    private Integer last10MonthTrend;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}