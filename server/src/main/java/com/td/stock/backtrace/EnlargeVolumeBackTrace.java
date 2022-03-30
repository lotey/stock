/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.backtrace;

import com.alibaba.fastjson.JSON;
import com.td.common.common.GlobalConstant;
import com.td.common.mapper.RecommandMapper;
import com.td.common.model.Quotation;
import com.td.common.model.Recommand;
import com.td.common.util.SpiderUtil;
import com.td.stock.vo.BackTraceParamVo;
import com.td.stock.vo.BackTraceVo;
import com.td.stock.vo.VolumeVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @auther lotey
 * @date 6/1/20 8:53 PM
 * @desc 地位放量回溯
 */
@Component
@Slf4j
public class EnlargeVolumeBackTrace extends BaseBackTrace {

    @Autowired
    private RecommandMapper recommandMapper;

    @Override
    public List<BackTraceParamVo> fetchTraceParamList(Date date, int days) {
        SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = ymdFormat.format(date);
        List<Recommand> recommandList = recommandMapper.selectByDateCount(strDate,days,GlobalConstant.RECOMMAND_TYPE_VOLUME);
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
