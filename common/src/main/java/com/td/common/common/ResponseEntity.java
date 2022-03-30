/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Map;

/**
 * @auther lotey
 * @date 2019/5/5 17:53
 * @desc 通用请求返回体
 */
@Data
@AllArgsConstructor
public class ResponseEntity {

    private int code;

    private Map<String, String> headerMap = null;

    private String content;
}
