/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.mapper;

import com.td.common.common.BaseMapper;
import com.td.common.model.DictProp;
import com.td.common.vo.IdpCodesVo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface DictPropMapper extends BaseMapper<DictProp> {

    @Select("SELECT * FROM t_dict_prop")
    List<DictProp> selectAll();

    @Delete("DELETE FROM t_dict_prop")
    void deleteAll();

    @Select("SELECT industry as idyName,GROUP_CONCAT(`code`) as codes,0 as type FROM t_dict_prop WHERE industry IS NOT NULL AND industry <> '' GROUP BY industry" +
            " UNION ALL" +
            " SELECT idy_segment as idyName,GROUP_CONCAT(`code`) as codes,1 as type FROM t_dict_prop WHERE idy_segment IS NOT NULL AND idy_segment <> '' GROUP BY idy_segment")
    List<IdpCodesVo> selectIdyCodes();
}