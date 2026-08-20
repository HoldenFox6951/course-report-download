package dev.learning.reports;

import dev.learning.reports.config.InfraiProperties;
import java.net.http.HttpClient;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(InfraiProperties.class)
public class CourseReportApplication {
    public static void main(String[] args) {
        SpringApplication.run(CourseReportApplication.class, args);
    }

    @Bean
    HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
