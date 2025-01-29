package org.tukma.utils;

import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.io.InputStream;

@Controller
@RequestMapping("/debug")
public class TestAndDebugging {



    Environment environment;
    ResourceLoader resourceLoader;

    public TestAndDebugging(Environment environment, ResourceLoader resourceLoader) {
        this.environment = environment;
        this.resourceLoader = resourceLoader;
    }

    @GetMapping("/test")
    public void test() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:static/sample.mp3");
        InputStream stream = resource.getInputStream();
        String key = environment.getProperty("openai.key");
        System.out.println("THE KEY IS " + key);



    }





}
