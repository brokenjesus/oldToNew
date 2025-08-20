package by.lupach.oldtonew.configs;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "app.old-system")
public class AppProps {
    @Value("${app.old-system-base-url}")
    private String baseUrl;

    @Value("${app.lookback-years}")
    private int lookbackYears;

}