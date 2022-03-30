/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.common;

/**
 * @auther lotey
 * @date 2019/5/22 9:05
 * @desc 父类Mapper，封装公用方法
 */
public interface BaseMapper<T> {

    int deleteByPrimaryKey(Integer id);

    int insert(T record);

    int insertSelective(T t);

    T selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(T record);

    int updateByPrimaryKey(T t);
}
