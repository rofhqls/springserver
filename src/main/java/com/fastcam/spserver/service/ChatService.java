package com.fastcam.spserver.service;

import com.fastcam.spserver.dto.RequestDto;
import com.fastcam.spserver.dto.ResponseDto;
import com.fastcam.spserver.entity.ChatList;
import com.fastcam.spserver.entity.ChatMessage;
import com.fastcam.spserver.repository.ChatListRepository;
import com.fastcam.spserver.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ChatService {

    @Autowired
    ChatMessageRepository cmr;

    private final RestClient restClient;
    HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofMinutes(5));
    ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
    public ChatService() {
        this.restClient = RestClient.builder()
                .requestFactory(new ReactorClientHttpRequestFactory(httpClient))
                .baseUrl("http://43.201.28.68:8000")
                .build();
    }

    public ResponseDto chatProcess(RequestDto req) {

        // 1. 전달된 세션아이디로 기존 대화 히스토리 DB 조회
        List<ChatMessage> historyEntites = cmr.findBySessionIdOrderByCreatedAtAsc( req.getSessionId() );

        // 2. FastAPI로 전달할  이전 대화 히스토리 리스트 변환
        // List<ChatMessage> 의 각 ChatMessage에서 sender와 content 만  Map<String, String> 저장하고
        // 이들을 List에 담습니다
        List<Map<String, String>> historyList = new ArrayList< Map<String, String> >();
        for( ChatMessage cm : historyEntites){
            Map<String , String > map = new HashMap<>();
            map.put("sender", cm.getSender() );
            map.put("content", cm.getContent() );
            historyList.add(map);
        }

        // 3. 현재 요청중인 사용자의 메시지와 sessionId로 DB 에 레코드를 추가
        ChatMessage chatmessage = new ChatMessage();
        chatmessage.setSessionId( req.getSessionId() );
        chatmessage.setContent( req.getMessage() );
        chatmessage.setSender( "USER" );
        cmr.save( chatmessage );


        // 4.  FastAPI에 보낼 양식으로 현재 요청 데이터를  재구성
        // Map<String, Object> 으로 변환
        Map<String, Object> fastApiReq = new HashMap<>();
        fastApiReq.put("session_id", req.getSessionId());
        fastApiReq.put("message", req.getMessage());
        fastApiReq.put("history", historyList);

        // 5. RestClient를 이용한 FastAPI 호출
        Map response = restClient.post()
                .uri("/process")
                .contentType(MediaType.APPLICATION_JSON)
                .body( fastApiReq )
                .retrieve()
                .body(Map.class);
        ResponseDto rdto = new ResponseDto();
        if( response != null){
            rdto.setMessage((String) response.get("message"));
            rdto.setFileName((String) response.get("file_name"));
            rdto.setFileUrl((String) response.get("file_url"));
        }else{
            rdto.setMessage("");
            rdto.setFileName(null);
            rdto.setFileUrl(null);
        }

        // 6. AI 응답 메시지 및 파일 정보 DB 저장
        ChatMessage aimessage = new ChatMessage();
        aimessage.setSessionId( req.getSessionId() );
        aimessage.setSender("AI");
        aimessage.setContent( rdto.getMessage() );
        aimessage.setFileName( rdto.getFileName() );
        aimessage.setFileUrl( rdto.getFileUrl() );
        cmr.save( aimessage );

        // 7. 최종 RssponseDto를 리턴
        return rdto;
    }

    @Autowired
    ChatListRepository clr;

    @Transactional
    public void saveSessionId(String sessionId) {
        ChatList cl = clr.findBySessionId( sessionId );
        try {
            if (cl == null) {
                ChatList dto = new ChatList();
                dto.setSessionId(sessionId);
                clr.save(dto);
            }
        }catch(Exception e){

        }
    }


    public List<String> getChatList() {
        List<String> sessionIdList = new ArrayList<>();

        List<ChatList> chatList = clr.findAll();
        for( ChatList cl : chatList ){
            sessionIdList.add( cl.getSessionId() );
        }
        return sessionIdList;
    }

    public List<ResponseDto> getHistory(String sessionId) {

        // sissionId로 대화목록을 조회
        List<ChatMessage> list = cmr.findBySessionIdOrderByCreatedAtAsc(sessionId);
        System.out.println("list.size()" + list.size() + "sessionId" + sessionId);

        // 하나하나 ResponseDto에 넣어서  리스트를 만들고 리턴
        List<ResponseDto> rdtoList = new ArrayList<>();

        for( ChatMessage chatmessage : list){
            ResponseDto rdto = new ResponseDto();
            rdto.setSender( chatmessage.getSender() );
            rdto.setMessage( chatmessage.getContent() );
            rdto.setFileName( chatmessage.getFileName() );
            rdto.setFileUrl( chatmessage.getFileUrl() );
            rdtoList.add( rdto );
        }
        return rdtoList;
    }
}
