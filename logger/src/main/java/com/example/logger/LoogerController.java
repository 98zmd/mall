package com.example.logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import common.MallConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController //Controller+responsebody
public class LoogerController {

    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;

    @GetMapping("1")
    public String doOne(){
        return "hello";
    }

    @PostMapping("log")
    public String doLog(@RequestParam("logString") String logString ){

        // 0 补充时间戳
        JSONObject jsonObject = JSON.parseObject(logString);
        jsonObject.put("ts",System.currentTimeMillis());
        // 1 落盘 file
        String jsonString = jsonObject.toJSONString();
        log.info(jsonString);


        // 2 推送到kafka
        if( "startup".equals( jsonObject.getString("type"))){
            kafkaTemplate.send(MallConstants.KAFKA_TOPIC_STARTUP,jsonString);
        }else{
            kafkaTemplate.send(MallConstants.KAFKA_TOPIC_EVENT,jsonString);
        }

        return "success";
    }
}