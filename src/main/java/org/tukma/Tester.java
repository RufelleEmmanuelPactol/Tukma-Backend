package org.tukma;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class Tester {

    @GetMapping("/heartbeat")
    public ResponseEntity<String> runControl() {
        return new ResponseEntity<>("server-alive", HttpStatus.OK);
    }
}
