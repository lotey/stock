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
public class DictProp {
    private Long id;

    private String code;

    private String name;

    private String companyName;

    private String province;

    private String idySegment;

    private String industry;

    private String plate;

    private BigDecimal lyr;

    private BigDecimal ttm;

    private String ceo;

    private String artificialPerson;

    private String address;

    private Date latestLiftBan;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}