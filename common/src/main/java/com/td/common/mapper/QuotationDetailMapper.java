/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.mapper;

import com.td.common.common.BaseMapper;
import com.td.common.model.QuotationDetail;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.LinkedList;
import java.util.List;

public interface QuotationDetailMapper extends BaseMapper<QuotationDetail> {

    @Delete("DELETE FROM t_quotation_detail WHERE date = #{date}")
    int deleteByDate(@Param("date") String date);

    @Select("SELECT * FROM t_quotation_detail WHERE date = #{date}")
    List<QuotationDetail> selectListByDate(@Param("date") String date);

    @Select("SELECT * FROM t_quotation_detail WHERE code = #{code} ORDER BY date")
    LinkedList<QuotationDetail> selectListByCode(@Param("code") String code);

    @Delete("DELETE FROM t_quotation_detail WHERE code = #{code}")
    int deleteByCode(@Param("code") String code);

    @Select("SELECT * FROM t_quotation_detail WHERE date = #{date} AND code IN(${codes})")
    List<QuotationDetail> selectListByDateAndCodes(@Param("date") String date,@Param("codes") String codes);

    @Select("SELECT COUNT(1) FROM t_quotation_detail WHERE date = #{date}")
    int selectCountByDate(@Param("date") String date);

    @Select("SELECT date FROM t_quotation_detail WHERE date > '2020-01-20' GROUP BY date")
    List<String> selectAviDateList();
}