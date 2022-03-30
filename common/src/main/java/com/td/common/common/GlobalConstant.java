/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.common;

import java.util.HashMap;
import java.util.Map;

/**
 * @auther lotey
 * @date 2019/7/26 14:29
 * @desc 功能描述
 */
public class GlobalConstant {

    //HTTP请求方式
    public static final String HTTP_GET = "GET";
    public static final String HTTP_POST = "POST";
    public static final String HTTP_POST_JSON = "POST_JSON";

    //批量添加修改值
    public static final int BATCH_MODDEL_INSERT = 0;
    public static final int BATCH_MODDEL_UPDATE = 1;

    //涨跌趋势
    public static final String TREND_UP = "1";
    public static final String TREND_DOWN = "2";

    //默认全局header
    public static Map<String,String> DEFAULTHEADERMAP = new HashMap<>();
    static {
        DEFAULTHEADERMAP.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.71 Safari/537.36");
    }

    //新浪全局header
    public static Map<String,String> DEFAULTSINAHEADER = new HashMap<>();
    static {
        DEFAULTSINAHEADER.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.71 Safari/537.36");
        DEFAULTSINAHEADER.put("Referer","https://gu.sina.cn/m/");
    }

    //原网页编码格式
    public static final String CHARASET_UTF8 = "utf-8";
    public static final String CHARASET_GBK = "gbk";

    //新浪实时行情数据URL，可同时获取多个，如下
    //http://hq.sinajs.cn/list=sh600000,sh600106
    public static final String SINA_STOCK_QUOTATION_URL = "http://hq.sinajs.cn/list=%s";
    public static final String MOCK_SINA_STOCK_QUOTATION_URL = "http://106.13.127.169:5002/rest/mock/list=%s/%d";
    //腾讯行情数据接口
    //https://qt.gtimg.cn/q=s_sh600519,s_sh601398
    public static final String TENCENT_STOCK_QUOTATION_URL = "https://qt.gtimg.cn/q=%s";
    //新浪交易统计数据URL，市值，成交总金额，总笔数等
    public static final String SINA_STOCK_EXTRA_URL = "https://hq.sinajs.cn/?=%s&list=%s,%s_i";
    //新浪交易统计数据URL，市值，成交总金额，总笔数等，不包含行情数据，一次可获取多个用逗号分隔，如下
    //https://hq.sinajs.cn/?=sh600001,sh600002&list=sh600001_i,sh600002_i
    public static final String SINA_STOCK_I_URL = "https://hq.sinajs.cn/?=%s&list=%s";
    //新浪均价数据URL
    public static final String SINA_AVG_DATA_URL = "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=%s&scale=240&ma=5,10,20,30&datalen=%s";
    //同花顺股票基本数据
    //https://basic.10jqka.com.cn/000651/
    public static final String JQKA_STOCK_BASIC_URL = "https://basic.10jqka.com.cn/%s/";
    //同花顺股票公司数据
    //https://basic.10jqka.com.cn/000651/company.html
    public static final String JQKA_STOCK_COMPANY_URL = "https://basic.10jqka.com.cn/%s/company.html";
    //网易股票excel统计URL
    public static final String NETEASE_EXCEL_DOWNLOAD_URL = "http://quotes.money.163.com/service/chddata.html?code=%s&start=%s&end=%s&fields=TCLOSE;HIGH;LOW;TOPEN;LCLOSE;CHG;PCHG;TURNOVER;VOTURNOVER;VATURNOVER;TCAP;MCAP";
    //米铺代理抓取URL
    public static final String MIMVP_PROXY_URL = "https://proxy.mimvp.com/freesecret?n=%s";
    public static final String MIMVP_FREE_PROXY_URL = "https://proxy.mimvp.com/freeopen?n=%s";
    //熊猫代理提取URL
    public static final String XIONGMAO_PROXY_URL = "http://route.xiongmaodaili.com/xiongmao-web/api/glip?secret=%s&orderNo=%s&count=%s&isTxt=1&proxyType=1";
    //可转债市值和剩余规模URL
    //抓取来源集思录，参考地址：https://www.jisilu.cn/data/cbnew/#cb
    public static final String KZZ_JISILU_ISS_URL = "https://www.jisilu.cn/data/cbnew/cb_list/?___jsl=LST___t=%s";

    //最大线程数
    public static final int MAX_THREAD_COUNT = 8;

    //十字星最大阀值
    public static final double DOJI_LIMIT_NUM = 0.006;
    //早晨十字星最大阀值
    public static final double MAX_DOJI_LIMIT_NUM = 0.003;
    //最低交易金额
    public static final int MIN_TRADE_AMOUNT = 70000000;
    //最小股价
    public static final double MIN_STOCK_PRICE = 3;
    //快速涨跌阀值
    public static final double MIN_CHANGE_LIMIT_PRICE = 0.002;

    //黑名单列表字典key
    public static final String SYS_DICT_TYPE_BACKLIST = "backlist";
    //优质股key
    public static final String SYS_DICT_TYPE_QUALITY_STOCKS = "quality_stocks";
    //优质可转债key
    public static final String SYS_DICT_TYPE_QUALITY_KZZ = "quality_kzz";

    //股票字典类型-股票
    public static final int DICT_TYPE_STOCK = 0;
    //股票字典类型-可转债
    public static final int DICT_TYPE_KZZ = 1;

    //推荐放量
    public static final String RECOMMAND_TYPE_VOLUME = "1";
    //推荐放量回撤
    public static final String RECOMMAND_TYPE_BACK = "2";

    //股票参考，用于确定上个和上N个交易日，以工商银行为参考
    public static final String STOCK_REFER_CODE = "sh601398";

    //抓取股票类型，全部和涨停的和代表性的
    public static final Integer CREW_STOCK_ALL = 0;
    public static final Integer CREW_STOCK_POOL = 1;
    public static final Integer CREW_STOCK_REFER = 2;
    public static final Integer CREW_STOCK_MAIN_BOARD = 3;

    //监控股票类型
    public static final String MONITOR_TYPE_A1 = "A_1";
    public static final String MONITOR_TYPE_A2 = "A_2";
    public static final String MONITOR_TYPE_A3 = "A_3";
    public static final String MONITOR_TYPE_A4 = "A_4";
    public static final String MONITOR_TYPE_B = "B_1";
    public static final String MONITOR_TYPE_C = "C_1";
    public static final String MONITOR_TYPE_D = "D_1";

    //股票行业概念关系类型
    public static final String CODE_IDP_TYPE_IDY = "0";
    public static final String CODE_IDP_TYPE_IDY_SEG = "1";
    public static final String CODE_IDP_TYPE_P = "2";
}
