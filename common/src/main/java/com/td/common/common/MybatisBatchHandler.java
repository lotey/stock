/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * @auther lotey
 * @date 2019/5/26 16:41
 * @desc mybatis批处理执行器，使用此执行器时，Mapper必须继承BaseMapper接口
 */
@Component
@Slf4j
public class MybatisBatchHandler {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    /**
     * 批量添加或修改数据记录
     * @param recordList 需要添加的E列表
     * @param clazz Maper的class类
     * @param mod 添加或修改，0：添加；1：修改
     * @param <E>
     * @param <T>
     */
    public <E,T extends BaseMapper<E>> void batchInsertOrUpdate(List<E> recordList, Class<T> clazz, int mod) {
        if (recordList != null && recordList.size() > 0) {
            SqlSession sqlSession = null;
            try {
                sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH,false);
                T mapper = sqlSession.getMapper(clazz);
                int batchCount = 50000;//提交数量,到达这个数量就提交
                for (int index = 0; index < recordList.size(); index++) {
                    if (mod == 0) {
                        mapper.insertSelective(recordList.get(index));
                    } else {
                        mapper.updateByPrimaryKeySelective(recordList.get(index));
                    }
                    if(index != 0 && index % batchCount == 0){
                        sqlSession.commit();
                    }
                }
                sqlSession.commit();
            } catch (Exception e){
                e.printStackTrace();
                log.error("===============批量添加列表失败===============");
                if(sqlSession != null){
                    sqlSession.rollback();
                }
            } finally {
                if(sqlSession != null){
                    sqlSession.close();
                }
            }
        }
    }
}
