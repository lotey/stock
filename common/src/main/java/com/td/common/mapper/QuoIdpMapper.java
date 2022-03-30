/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.mapper;

import com.td.common.common.BaseMapper;
import com.td.common.model.QuoIdp;
import com.td.common.vo.CodeIdpsVo;
import com.td.common.vo.IdpCodesVo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface QuoIdpMapper extends BaseMapper<QuoIdp> {

    @Delete("DELETE FROM t_quo_idp")
    void deleteAll();

    @Select("SELECT code,GROUP_CONCAT(idp_name) as idpNames FROM t_quo_idp WHERE type IN(0,1,2) GROUP BY `code`")
    List<CodeIdpsVo> selectCodeIdpsList();

    @Select("SELECT idp_name as idpName,GROUP_CONCAT(DISTINCT `code`) as codes FROM t_quo_idp GROUP BY idp_name")
    List<IdpCodesVo> selectIdpCodesList();
}