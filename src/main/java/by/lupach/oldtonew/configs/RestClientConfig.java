package by.lupach.oldtonew.configs;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {
    @Bean
    public RestTemplate oldSystemRestTemplate(RestTemplateBuilder builder, AppProps props){
        return builder
                .rootUri(props.getBaseUrl())
                .build();
    }
}