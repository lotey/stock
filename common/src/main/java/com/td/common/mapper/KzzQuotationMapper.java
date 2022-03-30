/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.mapper;

import com.td.common.common.BaseMapper;
import com.td.common.model.KzzQuotation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

public interface KzzQuotationMapper extends BaseMapper<KzzQuotation> {

    @Delete("DELETE FROM t_kzz_quotation WHERE date = #{date}")
    void deleteByDate(@Param("date") String date);

    @Select("SELECT * FROM t_kzz_quotation WHERE date = #{date}")
    List<KzzQuotation> selectListByDate(@Param("date") String date);
}