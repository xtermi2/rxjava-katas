package com.senacor.codecamp.reactive.services.statistics;

import com.senacor.codecamp.reactive.services.statistics.external.ArticleReadEvent;
import com.senacor.codecamp.reactive.services.statistics.external.ArticleReadEventsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Andri Bremm
 */
public class ArticleReadEventsServiceTest {

    private ArticleReadEventsService articleReadEventsService;

    private RequestHeadersSpec headerSpec;

    @Before
    public void setUp() {
        headerSpec = mock(RequestHeadersSpec.class, Mockito.RETURNS_SELF);
        RequestHeadersUriSpec uriSpec = mock(RequestHeadersUriSpec.class, invocation -> headerSpec);
        WebClient webClient = mock(WebClient.class, invocation -> uriSpec);

        articleReadEventsService = new ArticleReadEventsService(webClient);
    }

    @Test
    public void fetchReadEvents() {
        Flux<ArticleReadEvent[]> flux = Flux.interval(Duration.ofMillis(30)).take(3)
                .map(count -> asList(createReadEvent()).toArray(new ArticleReadEvent[]{}));
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.bodyToFlux(ArticleReadEvent[].class)).thenReturn(flux);
        when(headerSpec.exchange()).thenReturn(Mono.just(clientResponse));

        Flux<ArticleReadEvent> result = articleReadEventsService.readEvents();

        StepVerifier.create(result)
                .expectNext(createReadEvent())
                .expectNext(createReadEvent())
                .expectNext(createReadEvent())
                .verifyComplete();
    }

    private ArticleReadEvent createReadEvent() {
        return new ArticleReadEvent("name", 100);
    }

}
