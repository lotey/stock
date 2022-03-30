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
public class Quotation {
    private Long id;

    private String code;

    private String name;

    private LocalDate date;

    private BigDecimal current;

    private BigDecimal init;

    private BigDecimal open;

    private BigDecimal high;

    private BigDecimal low;

    private BigDecimal close;

    private BigDecimal volume;

    private BigDecimal volumeAmt;

    private BigDecimal offsetRate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String sourceData;
}