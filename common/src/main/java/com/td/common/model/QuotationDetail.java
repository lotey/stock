/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.model;

import lombok.Data;
import java.util.Date;

@Data
public class QuotationDetail {
    private Long id;

    private String code;

    private String name;

    private Date date;

    private String data1;

    private String data2;

    private String data3;

    private String data4;

    private Date createTime;

    private Date updateTime;
}