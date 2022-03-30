/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Date;

/**
 * @auther lotey
 * @date 2019/7/26 23:54
 * @desc 行情数据传输载体
 */
@Data
@AllArgsConstructor
public class QuotationVo {

    private String code;

    private String name;

    private Date date;

    private String data;

}
