package com.zhongweixian;

import com.zhongweixian.service.OssService;
import com.zhongweixian.service.WeiBoHttpService;
import com.zhongweixian.web.entity.BotVideo;
import com.zhongweixian.web.entity.page.Page;
import com.zhongweixian.web.service.BotVideoService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = WechatBootApplication.class)
@EnableAutoConfiguration
public class DemoApplicationTests {
    Logger logger = LoggerFactory.getLogger(DemoApplicationTests.class);

    @Autowired
    private WeiBoHttpService weiBoHttpService;

    @Autowired
    private BotVideoService botVideoService;

    @Autowired
    private OssService ossService;

    private ExecutorService task = Executors.newCachedThreadPool();


    String cookie = "SINAGLOBAL=9456802003620.64.1561629285945; SSOLoginState=1564105595; _s_tentry=login.sina.com.cn; Apache=5834488734170.566.1564105601160; ULV=1564105601963:16:14:3:5834488734170.566.1564105601160:1563845497227; UOR=login.sina.com.cn,weibo.com,club.huawei.com; SCF=Aqqh9eM0QX_5TJHvIT1Bp_DVlqjNkTXae_JopKR-sMiuO_WAICzBHHH94WX9Yt5N6bM7XSI9df-jngoQ0TBgBnQ.; SUB=_2A25wOXw9DeRhGeFP4lYZ9CjPyDWIHXVTT-r1rDV8PUNbmtBeLVHCkW9NQO_ryjRN7PPdiACZXu9KkTRyxutlmpWh; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9Whu6d1rS1RzpY.o0E_MyWMw5JpX5KMhUgL.FoMp1KBRShq0e0.2dJLoIEXLxK-L12qL12BLxKML1hnLB-eLxKqL1-eLB.2LxK-L1K.LBKnLxKBLB.2LB.2t; SUHB=0P1cPNQhOSWiFD; ALF=1595817964; wvr=6; webim_unReadCount=%7B%22time%22%3A1564282245741%2C%22dm_pub_total%22%3A0%2C%22chat_group_client%22%3A1164%2C%22allcountNum%22%3A1164%2C%22msgbox%22%3A0%7D";

    @Test
    public void uploadVideo() {
        Map<String, Object> param = new HashMap<>();
        param.put("status", 1);
        param.put("pageNum", 0);
        param.put("limit", 500);
        Page<BotVideo> page = botVideoService.findByPageParams(param);

        for (BotVideo botVideo : page.getList()) {
            if (botVideo.getStatus() == 2) {
                continue;
            }
        }


    }


}
