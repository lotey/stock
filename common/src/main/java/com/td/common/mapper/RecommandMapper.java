/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.mapper;

import com.td.common.common.BaseMapper;
import com.td.common.model.Recommand;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface RecommandMapper extends BaseMapper<Recommand> {

    @Delete("DELETE FROM t_recommand WHERE date = #{date} AND type = #{type}")
    int deleteByDate(@Param("date") String date,@Param("type") String type);

    @Select("SELECT * FROM t_recommand WHERE date = #{date} AND type = #{type}")
    Recommand selectByDate(@Param("date") String date,@Param("type") String type);

    @Select("SELECT * FROM t_recommand WHERE type = #{type} AND date <= #{date} ORDER BY date DESC LIMIT #{count}")
    List<Recommand> selectByDateCount(@Param("date") String date, @Param("count") Integer count,@Param("type") String type);

    @Select("SELECT * FROM t_recommand WHERE type = #{type}")
    List<Recommand> selectAll(@Param("type") String type);

    @Delete("DELETE FROM t_recommand WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}