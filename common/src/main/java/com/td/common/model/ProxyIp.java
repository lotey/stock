/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.model;

import lombok.Data;
import java.util.Date;

@Data
public class ProxyIp {
    private Long id;

    private String ip;

    private Integer port;

    private String userName;

    private String password;

    private String type;

    private Date createTime;

    private Date updateTime;
}