/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.kzz.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @auther lotey
 * @date 2019/8/1 22:59
 * @desc 股票编码名称实体类
 */
@Data
@AllArgsConstructor
public class CodeNameVo {

    private String code;

    private String name;
}
