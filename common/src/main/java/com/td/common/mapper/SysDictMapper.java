/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.mapper;

import com.td.common.common.BaseMapper;
import com.td.common.model.SysDict;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface SysDictMapper extends BaseMapper<SysDict> {

    @Select("SELECT * FROM sys_dict WHERE del_flag = 0 AND type = #{type}")
    List<SysDict> selectByType(@Param(value = "type") String type);

    @Delete("DELETE FROM sys_dict WHERE type = #{type}")
    void deleteByType(@Param(value = "type") String type);
}