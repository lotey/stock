/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.mapper;

import com.td.common.common.BaseMapper;
import com.td.common.model.UpDown;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface UpDownMapper extends BaseMapper<UpDown> {

    @Delete("DELETE FROM t_up_down WHERE date = #{date}")
    void deleteByDate(@Param(value = "date") String date);

    @Select("SELECT * FROM t_up_down WHERE date >= #{minDate} AND date <= #{maxDate}")
    List<UpDown> selectListByDateRange(@Param("minDate") String minDate, @Param("maxDate") String maxDate);
}