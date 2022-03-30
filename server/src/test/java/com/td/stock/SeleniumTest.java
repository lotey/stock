package com.td.stock;

import org.apache.commons.io.IOUtils;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @auther lotey
 * @date 2021/3/27 19:09
 * @desc 功能描述
 */
public class SeleniumTest {

    public static void main(String []args) throws IOException {
        System.setProperty("webdriver.chrome.driver", "D:\\chromedriver\\chromedriver.exe");
        //初始化自动化抓取工具
        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--headless"); //无浏览器模式
        options.addArguments("--header-args");
        options.addArguments("--disable-gpu");
        options.addArguments("blink-settings=imagesEnabled=false");
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--no-sandbox"); //关闭沙盒模式
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.90 Safari/537.36");
        //不提示“Chrome正受到自动测试软件控制”
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        //忽略ssl错误
        options.setCapability("acceptSslCerts", true);
        options.setCapability("acceptInsecureCerts", true);
        //设置代理
//        options.addArguments("--proxy-server=http://" + String.format("%s:%s","127.0.0.1",1080));

        Map<String, Object> prefs = new HashMap<>();
        prefs.put(CapabilityType.ACCEPT_INSECURE_CERTS, true);
        prefs.put("profile.default_content_settings.popups", 0);
        prefs.put("profile.password_manager_enabled", false);
        // 禁止下载加载图片
        prefs.put("profile.managed_default_content_settings.images", 2);
        options.setExperimentalOption("prefs", prefs);

        ChromeDriver webDriver = new ChromeDriver(options);//实例化
        //隐藏特性
        Map<String,Object> parameters = new HashMap<>();
        InputStream is = SeleniumTest.class.getResourceAsStream("/file/stealth.min.js");
        byte []stealthBytes = IOUtils.toByteArray(is);
        parameters.put("source", new String(stealthBytes));
        webDriver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", parameters);
        //设置超时时间为3S
        webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(5));

        webDriver.get("https://www.taobao.com/");
//        webDriver.get("https://basic.10jqka.com.cn/601566/");
        String content = webDriver.getPageSource();
        System.out.println(content);
    }
}
