/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.mapper;

import com.td.common.common.BaseMapper;
import com.td.common.model.HisQuotation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.LinkedList;
import java.util.List;

public interface HisQuotationMapper extends BaseMapper<HisQuotation> {

    @Delete("DELETE FROM t_his_quotation")
    void deleteAll();

    @Delete("DELETE FROM t_his_quotation WHERE date = #{date} AND code = #{code}")
    void deleteByDateAndCode(@Param("date") String date,@Param("code") String code);

    @Delete("DELETE FROM t_his_quotation WHERE date = #{date}")
    void deleteByDate(@Param("date") String date);

    @Update("TRUNCATE TABLE t_his_quotation")
    void truncateHisData();

    @Select("SELECT * FROM t_his_quotation WHERE date = #{date} ORDER BY create_time ASC")
    List<HisQuotation> selectListByDate(@Param("date") String date);

    @Select("SELECT * FROM t_his_quotation WHERE date = #{date} AND count = #{count} AND code IN(${codes})")
    List<HisQuotation> selectListByDateAndCodes(@Param("date") String date,@Param("count") Integer count, @Param("codes") String codes);

    @Select("SELECT * FROM t_his_quotation WHERE date = #{date} AND code = #{code} ORDER BY create_time")
    LinkedList<HisQuotation> selectListByDateAndCode(@Param("date") String date, @Param("code") String code);

}