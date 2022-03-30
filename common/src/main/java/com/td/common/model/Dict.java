/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Dict {
    private Long id;

    private String code;

    private String name;

    private Integer type;

    private BigDecimal cirMarketValue;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}