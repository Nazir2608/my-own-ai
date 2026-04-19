package com.nazir.myownai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class MyOwnAiApplication {

    public static void main(String[] args) {
        ApplicationContext ctx =
                SpringApplication.run(MyOwnAiApplication.class, args);
    }
}