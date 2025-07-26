package dv.service.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {
        "dv.service.*",
        "dv.common.*"
})
@EnableAsync
@EnableScheduling
public class DvServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DvServiceApplication.class, args);
    }

}