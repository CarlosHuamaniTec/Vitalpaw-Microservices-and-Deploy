package com.vitalpaw.sensordataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitalpaw.sensordataservice.dto.SensorDataDTO;
import com.vitalpaw.sensordataservice.service.MqttService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/stream")
public class StreamingController {

    @Autowired
    private MqttService mqttService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping(value = "/sensor-data", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSensorData() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        AtomicBoolean isActive = new AtomicBoolean(true);

        emitter.onTimeout(() -> isActive.set(false));
        emitter.onError((e) -> isActive.set(false));
        emitter.onCompletion(() -> isActive.set(false));

        new Thread(() -> {
            while (isActive.get()) {
                try {
                    SensorDataDTO data = mqttService.dataQueue.poll();
                    if (data != null) {
                        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(data)));
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    emitter.completeWithError(e);
                    break;
                }
            }
        }).start();

        return emitter;
    }
}