package com.senacor.codecamp.reactive.services.wikiloader.config;

import com.senacor.codecamp.reactive.services.WikiService;
import com.senacor.codecamp.reactive.util.DelayFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author Andreas Keefer
 */
@Configuration
@Profile("mock")
public class MockConfig {

    @Bean
    public WikiService wikiServiceMock() {
        return WikiService.create(DelayFunction.withRandomDelay(50, 300));
    }
}
