/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.mapper;

import com.td.common.common.BaseMapper;
import com.td.common.model.AvgPrice;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface AvgPriceMapper extends BaseMapper<AvgPrice> {

    @Delete("DELETE FROM t_avg_price WHERE date = #{date}")
    void deleteByDate(@Param(value = "date") String date);

    @Select("SELECT * FROM t_avg_price WHERE date = #{date}")
    List<AvgPrice> selectListByDate(@Param("date") String date);

    @Select("SELECT * FROM t_avg_price WHERE date >= #{minDate} AND date <= #{maxDate}")
    List<AvgPrice> selectListByDateRange(@Param("minDate") String minDate, @Param("maxDate") String maxDate);

    @Select("SELECT * FROM t_avg_price WHERE date >= (SELECT MIN(date) minDate FROM (SELECT date FROM t_avg_price WHERE date <= #{maxDate} GROUP BY date ORDER BY date DESC LIMIT #{count}) tmp) AND date <= #{maxDate}")
    List<AvgPrice> selectListByRangeOfNDay(@Param("maxDate") String maxDate, @Param("count") Integer count);
}