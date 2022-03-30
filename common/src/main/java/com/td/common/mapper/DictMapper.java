/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.mapper;

import com.td.common.common.BaseMapper;
import com.td.common.model.Dict;
import com.td.common.vo.DictPropVo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.math.BigDecimal;
import java.util.List;

public interface DictMapper extends BaseMapper<Dict> {

    @Select("SELECT * FROM t_dict")
    List<Dict> selectAll();

    @Select("SELECT * FROM t_dict WHERE type = #{type}")
    List<Dict> selectListByType(@Param(value = "type") int type);

    @Delete("DELETE FROM t_dict WHERE type = #{type}")
    void deleteByType(@Param(value = "type") int type);

    @Select("SELECT * FROM t_dict WHERE type = 1 AND cir_market_value > #{minAmt} AND cir_market_value < #{maxAmt}")
    List<Dict> selectByLeftAmt(@Param("minAmt") BigDecimal minAmt, @Param("maxAmt") BigDecimal maxAmt);

    @Select("SELECT * FROM t_dict WHERE type = 1 AND EXISTS (SELECT 1 FROM sys_dict sd WHERE `code` = sd.`value` AND sd.type = 'quality_kzz')")
    List<Dict> selectQualityKzzList();

    @Select("SELECT d.`code`,d.`name`,d.cir_market_value,p.industry,p.plate FROM t_dict d LEFT JOIN t_dict_prop p ON d.`code` = p.`code` WHERE d.type = 0")
    List<DictPropVo> selectAllDictPropList();
}