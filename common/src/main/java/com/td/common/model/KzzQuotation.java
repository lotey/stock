/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Data
public class KzzQuotation {
    private Long id;

    private String code;

    private String name;

    private Date date;

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
}