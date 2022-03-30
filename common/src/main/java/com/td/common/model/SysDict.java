/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.model;

import lombok.Data;
import java.util.Date;

@Data
public class SysDict {
    private Long id;

    private String value;

    private String label;

    private String type;

    private String description;

    private Long sort;

    private Long parentId;

    private Long createBy;

    private Date createDate;

    private Long updateBy;

    private Date updateDate;

    private String remarks;

    private String delFlag;
}