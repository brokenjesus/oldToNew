package by.lupach.oldtonew2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OldToNew2Application {

    public static void main(String[] args) {
        SpringApplication.run(OldToNew2Application.class, args);
    }

}
