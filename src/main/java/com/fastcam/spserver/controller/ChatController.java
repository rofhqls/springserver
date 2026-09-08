package com.fastcam.spserver.controller;

import com.fastcam.spserver.dto.RequestDto;
import com.fastcam.spserver.dto.ResponseDto;
import com.fastcam.spserver.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@RestController
@CrossOrigin({"http://43.201.28.68:8000", "http://43.201.28.68:3000", "http://43.201.28.68"})
public class ChatController {

    @Autowired
    ChatService cs;


    private final RestClient restClient;
    HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofMinutes(5));
    ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

    public ChatController() {
        this.restClient = RestClient.builder()
                .requestFactory(new ReactorClientHttpRequestFactory(httpClient))
                .baseUrl("http://43.201.28.68:8000")
                .build();
    }

    @GetMapping("/test")
    public String index() {
        return "<h1>HELLO! HELLO! HELLO! HELLO! HELLO! HELLO!</h1>";
    }

    @PostMapping("/chat")
    public ResponseEntity<ResponseDto> chat(@RequestBody RequestDto req) {

        ResponseDto rdto = cs.chatProcess(req);
        return ResponseEntity.ok(rdto);

    }

    @PostMapping("/saveSessionId")
    public HashMap<String, Object> saveSessionId(@RequestParam("sessionId") String sessionId) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        cs.saveSessionId(sessionId);
        map.put("msg", "ok");
        return map;
    }

    @GetMapping("/getChatList")
    public HashMap<String, Object> getChatList() {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("chatList", cs.getChatList());
        return map;
    }


    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> download(@PathVariable String fileName) {
        ResponseEntity<byte[]> response = restClient.get()
                .uri("http://43.201.28.68:8000/download/" + fileName)
                .retrieve()
                .toEntity(byte[].class);

        MediaType type = response.getHeaders().getContentType();

        return ResponseEntity.ok()
                .contentType(type)
                .body(response.getBody());
    }


    @GetMapping("/getHistory/{sessionId}")
    public ResponseEntity<List<ResponseDto>> getHistory(@PathVariable String sessionId) {

        List<ResponseDto> list = cs.getHistory(sessionId);

        return ResponseEntity.ok(list);

    }

}
