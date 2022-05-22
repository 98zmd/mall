package zmd.project.publisher.service;

import zmd.project.publisher.mapper.DauMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PublisherServiceImpl implements PublisherService {

    @Autowired
    private DauMapper dauMapper;

    @Override
    public Integer getDauTotal(String date) {
        return dauMapper.selectDauTotal(date);
    }

    @Override
    public Map getDauTotalHourMap(String date) {

        //定义Map存放分时统计的数据
        HashMap<String, Long> hourDauMap = new HashMap<>();

        //查询Pheonix获取分时数据
        List<Map> list = dauMapper.selectDauTotalHourMap(date);

        //遍历list:map((LOGHOUR->09),(ct->895)),map((LOGHOUR->10),(ct->1053))
        for (Map map : list) {
            hourDauMap.put((String) map.get("LH"), (Long) map.get("CT"));
        }

        return hourDauMap;
    }
}
