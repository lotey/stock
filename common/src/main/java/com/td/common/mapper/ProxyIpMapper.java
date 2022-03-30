/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.mapper;

import com.td.common.common.BaseMapper;
import com.td.common.model.ProxyIp;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface ProxyIpMapper extends BaseMapper<ProxyIp> {

    @Select("SELECT * FROM t_proxy_ip WHERE type IN('HTTPS','HTTP/HTTPS') LIMIT #{size}")
    List<ProxyIp> getHttpsProxyIpList(@Param("size") Integer size);

    @Select("SELECT * FROM t_proxy_ip")
    List<ProxyIp> selectAll();

    @Delete("DELETE FROM t_proxy_ip")
    void deleteAll();
}