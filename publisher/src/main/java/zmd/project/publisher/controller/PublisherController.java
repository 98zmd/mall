package zmd.project.publisher.controller;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import zmd.project.publisher.service.PublisherService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
public class PublisherController {

    @Autowired
    private PublisherService publisherService;

    @GetMapping("1")
    public String doOne(){
        return "hello";
    }

    @GetMapping("realtime-total")
    public String getTotal(@RequestParam("date") String date) {

        //获取日活数据
        Integer dauTotal = publisherService.getDauTotal(date);

        ArrayList<Map> result = new ArrayList<>();

        //添加日活数据信息
        HashMap<String, Object> dauMap = new HashMap<>();
        dauMap.put("id", "dau");
        dauMap.put("name", "新增日活");
        dauMap.put("value", dauTotal);
        result.add(dauMap);

        //添加新增用户信息
        HashMap<String, Object> newMidMap = new HashMap<>();
        newMidMap.put("id", "new_mid");
        newMidMap.put("name", "新增设备");
        newMidMap.put("value", 233);
        result.add(newMidMap);

        return JSON.toJSONString(result);
    }

    @GetMapping("realtime-hours")
    public String getRealTimeHour(@RequestParam("id") String id, @RequestParam("date") String date) {

        //定义JSON对象封装结果数据
        JSONObject jsonObject = new JSONObject();

        if ("dau".equals(id)) {

            //获取传入时间的分时统计
            Map todayHourMap = publisherService.getDauTotalHourMap(date);
            jsonObject.put("today", todayHourMap);

            //将传入时间日期减一
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date yesterdayDate = DateUtils.addDays(sdf.parse(date), -1);
                String yesterdayStr = sdf.format(yesterdayDate);

                //获取传入时间日期减一日期的分时统计
                Map yesterdayHourMap = publisherService.getDauTotalHourMap(yesterdayStr);
                jsonObject.put("yesterday", yesterdayHourMap);

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return jsonObject.toString();
    }


}