/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.backtrace;

import com.alibaba.fastjson.JSON;
import com.td.common.common.GlobalConstant;
import com.td.common.mapper.RecommandMapper;
import com.td.common.model.Recommand;
import com.td.stock.vo.BackTraceParamVo;
import com.td.stock.vo.VolumeVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @auther lotey
 * @date 6/1/20 8:53 PM
 * @desc 强势股回溯
 */
@Component
@Slf4j
public class HotStockBackTrace extends BaseBackTrace {

    @Autowired
    private RecommandMapper recommandMapper;

    @Override
    public List<BackTraceParamVo> fetchTraceParamList(Date date, int days) {
        SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = ymdFormat.format(date);
        List<Recommand> recommandList = recommandMapper.selectByDateCount(strDate,days, GlobalConstant.RECOMMAND_TYPE_BACK);
        List<BackTraceParamVo> traceParamVoList = new ArrayList<>();
        recommandList.forEach(recommand -> {
            if (StringUtils.isNotEmpty(recommand.getDataList())) {
                List<VolumeVo> volumeVoList = JSON.parseArray(recommand.getDataList(), VolumeVo.class);
                volumeVoList.forEach(volumeVo -> {
                    traceParamVoList.add(new BackTraceParamVo(){{
                        setCode(volumeVo.getCode());
                        setName(volumeVo.getName());
                        setDate(ymdFormat.format(recommand.getDate()));
                    }});
                });
            }
        });
        return traceParamVoList;
    }
}
