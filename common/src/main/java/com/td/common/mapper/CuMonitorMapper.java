/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.mapper;

import com.td.common.common.BaseMapper;
import com.td.common.model.CuMonitor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

public interface CuMonitorMapper extends BaseMapper<CuMonitor> {

    @Delete("DELETE FROM t_cu_monitor WHERE date = #{date}")
    void deleteByDate(@Param("date") String date);

    @Delete("DELETE FROM t_cu_monitor")
    void deleteAll();

    @Select("SELECT * FROM t_cu_monitor WHERE date = #{date}")
    List<CuMonitor> selectListByDate(@Param("date") String date);

    @Select("SELECT * FROM t_cu_monitor WHERE status = 0")
    List<CuMonitor> selectNewestList();

    @Update("UPDATE t_cu_monitor SET STATUS = 1")
    void updateStatus();
}
