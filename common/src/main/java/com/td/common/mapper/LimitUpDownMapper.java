/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.mapper;

import com.td.common.common.BaseMapper;
import com.td.common.model.LimitUpDown;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.LinkedList;
import java.util.List;

public interface LimitUpDownMapper extends BaseMapper<LimitUpDown> {

    @Delete("DELETE FROM t_limit_up_down WHERE date = #{date}")
    void deleteByDate(@Param(value = "date") String date);

    @Select("SELECT * FROM t_limit_up_down WHERE date >= #{minDate} AND date <= #{maxDate}")
    List<LimitUpDown> selectListByDateRange(@Param("minDate") String minDate, @Param("maxDate") String maxDate);

    @Select("SELECT * FROM t_limit_up_down WHERE date = #{date}")
    LimitUpDown selectByDate(@Param(value = "date") String date);

    @Select("SELECT * FROM t_limit_up_down WHERE date <= #{maxDate} ORDER BY date DESC LIMIT #{count}")
    LinkedList<LimitUpDown> selectLastNList(@Param("maxDate") String maxDate, int count);
}