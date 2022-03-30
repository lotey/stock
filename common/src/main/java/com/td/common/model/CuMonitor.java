/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CuMonitor {

    private Long id;

    private String date;

    private String code;

    private String name;

    private String type;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
