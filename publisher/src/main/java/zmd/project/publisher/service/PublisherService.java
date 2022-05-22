package zmd.project.publisher.service;

import org.springframework.stereotype.Service;

import java.util.Map;

public interface PublisherService {
    public Integer getDauTotal(String date);

    public Map getDauTotalHourMap(String date);

}
