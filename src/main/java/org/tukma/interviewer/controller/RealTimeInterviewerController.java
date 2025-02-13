package org.tukma.interviewer.controller;
import com.google.common.cache.*;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.tukma.auth.models.UserEntity;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.tukma.globals.WsTickets;
import org.tukma.interviewer.repositories.InterviewRepository;
import org.tukma.interviewer.services.InterviewService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/api/v1/interviewer")
public class RealTimeInterviewerController extends BinaryWebSocketHandler{

    private Environment environment;
    private InterviewService interviewService;



    public RealTimeInterviewerController(Environment environment, InterviewService interviewRepository) {
        this.environment = environment;
        this.interviewService = interviewRepository;

    }

    // create a ticket, request initiation of websocket connection
    @GetMapping("/request-ws-connection")
    public ResponseEntity<?> requestWsConnection(){
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        int tempHash = currentUser.hashCode();
        String actualHash = LocalDateTime.now().toString() + "-" + tempHash;


        interviewService.createInterview(
                currentUser,
                null,
                null,
                null,
                actualHash
        );
        WsTickets.addTicket(actualHash, currentUser.getId());


        return ResponseEntity.ok(Map.of("ticket", actualHash));
    }

    // check initiation-status of websocket connection
    @GetMapping("/check-ws-connection")
    public ResponseEntity<?> checkWsConnection(@RequestParam String ticket){
        if(WsTickets.getTicket(ticket) != null){
            var requestingEntity = (UserEntity)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (!WsTickets.getTicket(ticket).equals( requestingEntity.getId())) {
                return ResponseEntity.ok(Map.of("status", "unauthorized"));
            }
            return ResponseEntity.ok(Map.of("status", "initiated"));
        }
        return ResponseEntity.ok(Map.of("status", "not-initiated"));
    }








}
