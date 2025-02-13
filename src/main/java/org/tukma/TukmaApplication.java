package org.tukma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.tukma.interviewer.StaticPrompts;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class TukmaApplication {


    public static void main(String[] args) {

        SpringApplication.run(TukmaApplication.class, args);
        System.out.println(StaticPrompts.generateSystemPrompt(List.of("What is your name?", "What is your age?", "What is your favorite color?"),
                "Accenture", "Software Engineer"));




    }

}
