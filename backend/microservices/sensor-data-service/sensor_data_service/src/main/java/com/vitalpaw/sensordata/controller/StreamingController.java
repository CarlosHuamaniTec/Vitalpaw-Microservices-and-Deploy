package com.vitalpaw.sensordataservice.controller;

import com.vitalpaw.sensordataservice.dto.SensorDataDTO;
import com.vitalpaw.sensordataservice.service.MqttService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

@RestController
@RequestMapping("/sensor-data/stream")
public class StreamingController {

    @Autowired
    private MqttService mqttService;

    @GetMapping(produces = "text/event-stream")
    public SseEmitter streamData() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        ConcurrentLinkedQueue<SensorDataDTO> dataQueue = mqttService.dataQueue; // Acceso directo (mejorar con diseño)
        new Thread(() -> {
            while (!emitter.isCompleted()) {
                SensorDataDTO data = dataQueue.poll();
                if (data != null) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("sensor-data")
                                .data(data));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                        break;
                    }
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
        return emitter;
    }
}