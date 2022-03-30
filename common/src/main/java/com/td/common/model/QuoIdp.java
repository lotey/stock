/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.model;

import lombok.Data;

import java.util.Date;

@Data
public class QuoIdp {
    private Long id;

    private String code;

    private String idpName;

    private String type;

    private String pIdyName;

    private Date createTime;

    private Date updateTime;
}