/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LimitUpDown {
    private Long id;

    private LocalDate date;

    private String upList;

    private String downList;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}