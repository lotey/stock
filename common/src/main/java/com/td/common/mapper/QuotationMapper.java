/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.mapper;

import com.td.common.common.BaseMapper;
import com.td.common.model.Quotation;
import com.td.common.vo.LastNPriceVo;
import com.td.common.vo.RelativePositionVo;
import com.td.common.vo.UpLimitCountVo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

public interface QuotationMapper extends BaseMapper<Quotation> {

    @Delete("DELETE FROM t_quotation WHERE date = #{date}")
    void deleteByDate(@Param("date") String date);

    @Select("SELECT * FROM t_quotation WHERE date = #{date}")
    List<Quotation> selectListByDate(@Param("date") String date);

    @Select("SELECT * FROM t_quotation WHERE date >= #{minDate} AND date <= #{maxDate}")
    List<Quotation> selectListByDateRange(@Param("minDate") String minDate, @Param("maxDate") String maxDate);

    @Select("SELECT * FROM t_quotation WHERE date >= (SELECT MIN(date) minDate FROM (SELECT date FROM t_quotation WHERE date <= #{maxDate} GROUP BY date ORDER BY date DESC LIMIT #{count}) tmp) AND date <= #{maxDate}")
    List<Quotation> selectListByRangeOfNDay(@Param("maxDate") String maxDate, @Param("count") Integer count);

    @Select("SELECT * FROM t_quotation WHERE code = #{code} and date >= #{minDate} order by date limit #{count}")
    List<Quotation> selectSingleAfterNDayList(@Param("code") String code, @Param("minDate") String minDate, @Param("count") Integer count);

    @Select("SELECT * FROM t_quotation q WHERE EXISTS (SELECT 1 FROM sys_dict d WHERE type = 'quality_stocks' AND q.`code` = d.`value`) AND q.date >= (SELECT MIN(date) minDate FROM (SELECT date FROM t_quotation WHERE date <= #{maxDate} GROUP BY date ORDER BY date DESC LIMIT #{count}) tmp) AND q.date <= #{maxDate}")
    List<Quotation> selectRecommandListByRangeOfNDay(@Param("maxDate") String maxDate, @Param("count") Integer count);

    @Select("SELECT q.`code`,q.`name`,(100 / (maxPrice - minPrice)) * (current - minPrice) as pRate FROM t_quotation q LEFT JOIN (" +
            "SELECT `code`,MIN(low) as minPrice,MAX(high) as maxPrice FROM t_quotation WHERE date > DATE_SUB(date,INTERVAL 3 MONTH) GROUP BY `code`" +
            ") tmp ON q.`code` = tmp.`code` WHERE q.date = #{maxDate}")
    List<RelativePositionVo> selectCurPosition(@Param("maxDate") String maxDate);

    @Select("SELECT code,MAX(`name`) as name,COUNT(1) as count FROM t_quotation WHERE date > date_sub(#{maxDate},interval #{days} DAY) AND offset_rate > 9.45 GROUP BY `code` HAVING(COUNT(1)) > #{count}")
    List<UpLimitCountVo> selectUpLimitGtN(@Param("days") int days, @Param("maxDate") LocalDate maxDate, @Param("count") int count);

    @Select("SELECT code,MAX(high) as maxPrice,MIN(low) as minPrice FROM t_quotation WHERE date > date_sub(#{maxDate},interval #{count} DAY) and date < #{maxDate} GROUP BY code")
    List<LastNPriceVo> selectLastNMaxPrice(@Param("maxDate") String maxDate, @Param("count") int count);

    @Select("SELECT * FROM t_quotation WHERE code = #{code} AND date <= #{maxDate} ORDER BY date DESC LIMIT #{count}")
    LinkedList<Quotation> selectLastNDateList(@Param("maxDate") String maxDate, @Param("count") int count, @Param("code") String code);

    @Select("SELECT code FROM t_quotation GROUP BY `code` HAVING COUNT(1) < 100")
    List<String> selectNewQuoList();

    @Select("SELECT `code` FROM t_quotation WHERE date > DATE_SUB(date,INTERVAL 1 YEAR) AND offset_rate > 9.7 GROUP BY `code` HAVING COUNT(1) < 4")
    List<String> selectInactiveQuoList();

    @Select("SELECT * FROM t_quotation q WHERE EXISTS (SELECT 1 FROM (SELECT `code`,MAX(date) as date FROM t_quotation WHERE date < CURDATE() GROUP BY `code` ORDER BY date DESC) tmp WHERE q.code = tmp.code AND q.date = tmp.date)")
    List<Quotation> selectLastDayQuoList();
}