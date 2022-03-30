/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.td.common.common.GlobalConstant;
import com.td.common.common.ResponseEntity;
import com.td.stock.config.BaiduConfig;
import com.td.common.util.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @auther lotey
 * @date 2020/1/6 21:57
 * @desc 调用百度的APK识别图片验证码
 */
@Component
@Slf4j
public class BaiduAPI {

    //百度oauth2获取token的URL
    private static final String ACCESSTOKENURL = "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=%s&client_secret=%s";

    //百度识别图片URL
    private static final String NUMBERSOCRURL = "https://aip.baidubce.com/rest/2.0/ocr/v1/numbers?access_token=%s";

    @Autowired
    private HttpClientUtil clientUtil;
    @Autowired
    private BaiduConfig baiduConfig;

    /**
     * 获取百度的accessToken
     * @return
     */
    public String getAccessToken() {
        String url = String.format(ACCESSTOKENURL,baiduConfig.getApkKey(),baiduConfig.getSecretKey());
        ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_POST, 0, url, null, GlobalConstant.DEFAULTHEADERMAP,GlobalConstant.CHARASET_UTF8);
        if (responseEntity.getCode() == 200) {
            JSONObject jsonObject = JSON.parseObject(responseEntity.getContent());
            if (jsonObject.containsKey("access_token")) {
                return jsonObject.getString("access_token");
            }
        }
        log.error("=======================获取百度access_token失败=======================");
        return null;
    }

    /**
     * 调用百度API识别图片
     * @param accessToken
     * @param localImgPath
     * @return
     */
    public String getOCRImgText(String accessToken,String localImgPath) {
        String url = String.format(NUMBERSOCRURL,accessToken);

        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Content-Type","application/x-www-form-urlencoded");
        headerMap.put("User-Agent","Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3100.0 Safari/537.36");

        Map<String,Object> paramMap = new HashMap<>();
//        String jpgImgPath = pngToJpgConvert(localImgPath);
        paramMap.put("image",transImgToBase64Encode(localImgPath));

        ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_POST, 0, url, paramMap, headerMap,GlobalConstant.CHARASET_UTF8);
        if (responseEntity.getCode() == 200) {
            log.info("=====================图片链接 => {} 百度识别结果 => {}",localImgPath,responseEntity.getContent());
            JSONObject jsonObject = JSON.parseObject(responseEntity.getContent());
            if (jsonObject.containsKey("error_code")) {
                log.error("=======================调用百度OCR识别失败=======================");
                return null;
            }
            return jsonObject.getJSONArray("words_result").getJSONObject(0).getString("words");
        }
        return null;
    }

    /**
     * 转换图片为base64并编码
     * @param imgPath
     * @return
     */
    private String transImgToBase64Encode(String imgPath) {
        InputStream in;
        byte[] data = null;
        // 读取图片字节数组
        try {
            in = new FileInputStream(imgPath);
            data = new byte[in.available()];
            in.read(data);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 对字节数组Base64编码
        Encoder encoder = Base64.getEncoder();
        String base64ImageStr = encoder.encodeToString(data);
        base64ImageStr = base64ImageStr.replaceAll("\r","");
        base64ImageStr = base64ImageStr.replaceAll("\n","");
        base64ImageStr = base64ImageStr.replaceAll("\\+","%2B");
        return base64ImageStr;
//        try {
//            return URLDecoder.decode(base64ImageStr, "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        return null;
    }

    /**
     * 将png转换为jpg格式
     * @param sourcePngImgPath
     * @return
     */
    private String pngToJpgConvert(String sourcePngImgPath) {
        BufferedImage bufferedImage;
        try {
            //read image file
            bufferedImage = ImageIO.read(new File(sourcePngImgPath));
            // create a blank, RGB, same width and height, and a white background
            BufferedImage newBufferedImage = new BufferedImage(bufferedImage.getWidth(),bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            //TYPE_INT_RGB:创建一个RBG图像，24位深度，成功将32位图转化成24位
            newBufferedImage.createGraphics().drawImage(bufferedImage, 0, 0, Color.WHITE, null);
            // write to jpeg file
            String targetJpgFile = String.format("%s%s%s.jpg",sourcePngImgPath.substring(0,sourcePngImgPath.lastIndexOf(File.separator)),File.separator, UUID.randomUUID().toString());
            ImageIO.write(newBufferedImage, "jpg", new File(targetJpgFile));
            return targetJpgFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
