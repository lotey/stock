/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * @auther lotey
 * @date 2020/1/8 21:05
 * @desc 放量记录实体
 */
@Data
public class VolumeVo {

    private String code;

    private String name;

    private BigDecimal multiple;

    private String time;

    private BigDecimal volumeAmt;
}
