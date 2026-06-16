package com.openisle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OpenIsleApplication {

  public static void main(String[] args) {
    System.setProperty("jdk.tls.client.protocols", "TLSv1.2");
    System.setProperty("https.protocols", "TLSv1.2");
    SpringApplication.run(OpenIsleApplication.class, args);
  }
}
