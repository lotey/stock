/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.service;

import com.td.common.common.GlobalConstant;
import com.td.common.mapper.AvgPriceMapper;
import com.td.common.mapper.CuMonitorMapper;
import com.td.common.mapper.DictMapper;
import com.td.common.mapper.DictPropMapper;
import com.td.common.mapper.HisQuotationMapper;
import com.td.common.mapper.LimitUpDownMapper;
import com.td.common.mapper.ProxyIpMapper;
import com.td.common.mapper.QuoIdpMapper;
import com.td.common.mapper.QuotationMapper;
import com.td.common.mapper.RecommandMapper;
import com.td.common.mapper.SysDictMapper;
import com.td.common.mapper.UpDownMapper;
import com.td.common.model.CuMonitor;
import com.td.common.model.Dict;
import com.td.common.model.DictProp;
import com.td.common.model.LimitUpDown;
import com.td.common.model.Quotation;
import com.td.common.model.Recommand;
import com.td.common.model.SysDict;
import com.td.common.vo.CodeIdpsVo;
import com.td.common.vo.DictPropVo;
import com.td.common.vo.IdpCodesVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * @auther lotey
 * @date 2022/1/1 23:12
 * @desc 股票查询服务
 */
@Service
@Slf4j
public class StockQueryService {

    @Autowired
    private DictMapper dictMapper;
    @Autowired
    private QuotationMapper quotationMapper;
    @Autowired
    private LimitUpDownMapper limitUpDownMapper;
    @Autowired
    private UpDownMapper upDownMapper;
    @Autowired
    private AvgPriceMapper avgPriceMapper;
    @Autowired
    private SysDictMapper sysDictMapper;
    @Autowired
    private DictPropMapper dictPropMapper;
    @Autowired
    private ProxyIpMapper proxyIpMapper;
    @Autowired
    private RecommandMapper recommandMapper;
    @Autowired
    private CuMonitorMapper cuMonitorMapper;
    @Autowired
    private HisQuotationMapper hisQuotationMapper;
    @Autowired
    private QuoIdpMapper quoIdpMapper;

    /**
     * 查询全部的字典配置列表
     * @return
     */
    public List<SysDict> selectSysDictByType(String type) {
        return sysDictMapper.selectByType(type);
    }

    /**
     * 根据股票类型查询股票字典列表
     * @return
     */
    public List<Dict> selectDictListByType(int type) {
        return dictMapper.selectListByType(type);
    }

    /**
     * 查询全部字典基础数据
     * @return
     */
    public List<DictProp> selectAllDictProp() {
        return dictPropMapper.selectAll();
    }

    /**
     * 查询某天的行情列表
     * @param date
     * @return
     */
    public List<Quotation> selectListByDate(String date) {
        return quotationMapper.selectListByDate(date);
    }

    /**
     * 查询某天的涨停跌停列表
     * @param date
     * @return
     */
    public LimitUpDown selectLimitUpDownByDate(String date) {
        return limitUpDownMapper.selectByDate(date);
    }

    /**
     * 查询某天的放量列表
     * @param date
     * @return
     */
    public Recommand selectVolumeByDate(String date) {
        return recommandMapper.selectByDate(date, GlobalConstant.RECOMMAND_TYPE_VOLUME);
    }

    /**
     * 查询最新的涨停监控列表
     * @return
     */
    public List<CuMonitor> selectNewestMonitorList() {
        return cuMonitorMapper.selectNewestList();
    }

    /**
     * 查询上个交易日行情数据
     * @return
     */
    public List<Quotation> selectLastDayQuoList() {
        return quotationMapper.selectLastDayQuoList();
    }

    /**
     * 查询股票和行业映射关系列表
     * @return
     */
    public List<CodeIdpsVo> queryCodeIdpsList() {
        return quoIdpMapper.selectCodeIdpsList();
    }

    /**
     * 查询行业和股票映射关系列表
     * @return
     */
    public List<IdpCodesVo> queryIdpCodesList() {
        return quoIdpMapper.selectIdpCodesList();
    }

    /**
     * 根据股票类型查询股票字典列表
     * @return
     */
    public List<DictPropVo> selectAllDictPropList() {
        return dictMapper.selectAllDictPropList();
    }
}
