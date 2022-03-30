package com.td.stock;

import com.td.common.common.GlobalConstant;
import com.td.common.common.ResponseEntity;
import com.td.common.model.Dict;
import com.td.common.model.ProxyIp;
import com.td.common.model.Quotation;
import com.td.common.util.HttpClientUtil;
import com.td.stock.service.BaiduAPI;
import com.td.stock.service.ProxyTool;
import com.td.stock.service.StockQueryService;
import com.td.stock.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@SpringBootTest
public class StockApplicationTests {

    @Autowired
    private StockService stockService;
    @Autowired
    private StockQueryService stockQueryService;
    @Autowired
    private ProxyTool proxyTool;
    @Autowired
    private BaiduAPI baiduAPI;

    @Test
    public void testGenProxyZipFile() {
        proxyTool.genProxyZipPackage();
    }

    @Test
    public void testCheckAvaProxyIpList() {
        proxyTool.checkAvaProxyIpList();
    }

    @Test
    public void testCrewMimvpFreeProxyList() throws Exception {
        int loopCount = 5;
        int count = 0;
        while (count < loopCount) {
            proxyTool.crewMimvpFreeProxyList();
            Thread.sleep(1000L);
            count++;
            System.out.println("抓取第"+count+"次");
        }
        System.out.println("抓取结束");
    }

    @Test
    public void testGetBaiduAccessToken() {
        baiduAPI.getAccessToken();
    }

    @Test
    public void testGetOCRImgText() {
        baiduAPI.getOCRImgText(baiduAPI.getAccessToken(),"E:\\tmp\\75db054d-1f73-4b2b-8601-67613ef53f03.png");
    }

    @Test
    public void testDownNetEaseCSVFile() {
        String startDate = "20190701";
        String endDate = "20200612";
        List<Dict> dictList = stockQueryService.selectDictListByType(GlobalConstant.DICT_TYPE_STOCK);
        if (dictList != null && dictList.size() > 0) {
            dictList.forEach(dict ->{
                stockService.downNetEaseCSVFile(dict.getCode(),startDate,endDate);
            });
        }
    }

    @Test
    public void testCrewNetEaseStockList() {
        stockService.crewNetEaseStockList(LocalDate.now(),219);
    }

    @Test
    public void testSaveQuotationFromCSVFile() {
        stockService.saveQuotationFromCSVFile(null);
    }


    @Test
    public void testCrewMipu() {
        HttpClientUtil rClientUtil1 = new HttpClientUtil();
        ResponseEntity rntity = rClientUtil1.recursiveHttpRequest(GlobalConstant.HTTP_GET,0,"http://localhost:5001/rest/stock/crewMimvpProxyList/5",null,null,GlobalConstant.CHARASET_UTF8);
        System.out.println(rntity.getCode());
    }

    @Test
    public void testCrewSinaAvgList() throws Exception {
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        //stockService.crewHisSingAvgData(ymdFormat.parse("2020-01-23"),142);
        stockService.crewHisSingAvgDataByMutiThread(LocalDate.parse("2020-02-05",ymdFormatter),5);
    }

    @Test
    public void testSendDDMsg() {
        stockService.sendDingDingGroupMsg("测试");
    }

    @Test
    public void testCalcVolumeList() throws Exception {
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        stockService.calcVolumeList(LocalDate.parse("2020-02-28",ymdFormatter));
//        stockService.calcVolumeList(new Date());
    }

    @Test
    public void testSyncDictProp() {
        stockService.crewDictProp();
    }

    @Test
    public void testQueryRecommandList() throws Exception {
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Quotation> resultList = stockService.queryRecommandList(LocalDate.parse("2020-04-14",ymdFormatter));
        System.out.println(resultList);
    }

    @Test
    public void testMimvpProxy() {
        List<ProxyIp> proxyIpList = stockService.crewMimvpProxyList(8,1);
        ProxyIp proxyIp = proxyIpList.get(0);
        HttpClientUtil clientUtil = new HttpClientUtil();
        clientUtil.setMaxRetryCount(1);
        clientUtil.setProxyPropArr(new String[]{proxyIp.getIp(),String.valueOf(proxyIp.getPort())});
        String url = "http://localhost:5000/rest/stock/limitUpDownList/2020-07-15/1";
        ResponseEntity entity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0, url,null,null,GlobalConstant.CHARASET_UTF8);
        System.out.println(entity.getContent());
    }
}
