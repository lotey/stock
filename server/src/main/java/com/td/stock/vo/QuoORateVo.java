/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * @auther lotey
 * @date 2021/1/23 7:35 下午
 * @desc 功能描述
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuoORateVo {

    private String code;

    private List<Double> oRateList;
}
