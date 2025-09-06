package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.RequestLog;
import com.kevinmazali.portfolio.model.RequestLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestLogService {

    private final RequestLogRepository requestLogRepository;

    public RequestLogService(RequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    @Transactional
    public void save(String path, String method, String payload) {
        RequestLog log = new RequestLog();
        log.setPath(path);
        log.setMethod(method);
        log.setPayload(payload);
        requestLogRepository.save(log);
    }
}


