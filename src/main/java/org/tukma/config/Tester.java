package org.tukma.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Controller
@CrossOrigin(origins = "http://127.0.0.1:5500/")
@RequestMapping("api/v1/debug")
public class Tester {

    @GetMapping("/heartbeat")
    public ResponseEntity<String> runControl() {
        return new ResponseEntity<>("server-alive", HttpStatus.OK);
    }
    @GetMapping("/mono")
    public Mono<String> getMono() {
        return Mono.just("Hello, Mono!");
    }

    @GetMapping("/flux")
    public Flux<String> getFlux() {
        return Flux.just("Hello", "Reactive", "World!")
                .delayElements(Duration.ofMillis(30)); // Simulate streaming
    }


    @Autowired
    private Environment environment;

    @GetMapping("/test-var")
    public ResponseEntity<Map<String, String>> testingVariables () {
        HashMap<String, String> vars = new HashMap<>();
        String connVal = environment.getProperty("spring.datasource.url");

        vars.put("db-var-local", connVal);

        return new ResponseEntity<>(vars, HttpStatus.OK);
    }

    @GetMapping("/request-prime")
    @ResponseBody
    public SseEmitter requestPrime(@RequestParam(name = "n") String nx) {
        SseEmitter emitter = new SseEmitter();
        int n = Integer.parseInt(nx);

        new Thread(() -> {
            try {
                for (int i = 1; i < n; i++) {
                    int count = 0;
                    for (int j = i; j > 0; j--) {
                        if (i % j == 0) {
                            count++;
                        }
                    }
                    if (count <= 2) {
                        emitter.send(Integer.toString(i)); // Send each prime number
                        Thread.sleep(500);
                    }
                }
                emitter.complete(); // Close the connection when done
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter; // Return the SseEmitter to the client
    }



    @Autowired
    ResourceLoader resourceLoader;

    @GetMapping("/stream")
    public ResponseEntity<byte[]> streamVideo (@RequestHeader(value="Range", required = false) String range) throws Exception {
        String resourceLocation = "classpath:static/stream.mp4";
        Resource resource = resourceLoader.getResource(resourceLocation);
        byte[] videoBytes = Files.readAllBytes(resource.getFile().toPath());

        long length = videoBytes.length;

        long start = 0;
        long end = length - 1;

        if (range != null) {
            String[] ranges = range.replace("bytes=", "").split("-");
            start = Long.parseLong(ranges[0]);
            if (ranges.length > 1) {
                end = Long.parseLong(ranges[1]);

            }

        }

        end = start + 5000;
        end = Math.min(end, length - 1);
        byte[] neededRange = new byte[(int) (end - start + 1)];
        System.arraycopy(videoBytes, (int) start, neededRange, 0, neededRange.length);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Range", "bytes " + start + "-" + end + "/" + videoBytes.length);
        httpHeaders.add("Accept-Ranges", "bytes");
        httpHeaders.setContentType(MediaType.valueOf("video/mp4"));

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(httpHeaders)
                .body(neededRange);

    }
}
