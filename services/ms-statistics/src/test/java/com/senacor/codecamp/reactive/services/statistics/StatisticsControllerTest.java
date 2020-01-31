package com.senacor.codecamp.reactive.services.statistics;

import com.senacor.codecamp.reactive.services.statistics.external.ArticleMetricsService;
import com.senacor.codecamp.reactive.services.statistics.external.ArticleReadEvent;
import com.senacor.codecamp.reactive.services.statistics.external.ArticleReadEventsService;
import com.senacor.codecamp.reactive.services.statistics.model.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Andri Bremm
 */
public class StatisticsControllerTest {

    private static final String ARTICLE_BASE_URL = "${services.article.base-url}";
    private static final MediaType TEXT_EVENT_STREAM_UTF8 = new MediaType("text", "event-stream", StandardCharsets.UTF_8);

    private ArticleReadEventsService articleReadEventsServiceMock;
    private ArticleMetricsService articleMetricsServiceMock;

    private StatisticsController statisticsController;
    private WebTestClient testClient;

    @Before
    public void setUp() {
        this.articleReadEventsServiceMock = mock(ArticleReadEventsService.class);
        this.articleMetricsServiceMock = mock(ArticleMetricsService.class);
        this.statisticsController = new StatisticsController(articleReadEventsServiceMock, articleMetricsServiceMock);
        this.testClient = WebTestClient.bindToController(statisticsController).build();
    }

    @Test
    public void fetchArticleStatisticsWithDefaultUpdateInterval() {
        when(articleReadEventsServiceMock.readEvents())
                .thenReturn(Flux.interval(Duration.ofMillis(100)).take(6).map(count -> createReadEvent(count, 100))
                        .doOnNext(next -> System.out.println("readEvent: " + next)));
        when(articleMetricsServiceMock.fetchRatings(any())).thenAnswer(invocation -> {
            Flux<ArticleName> names = invocation.getArgument(0);
            return names
                    .doOnNext(name -> System.out.println("fetchRating: " + name))
                    .map(name -> new Rating(name.getName(), Integer.parseInt(name.getName())));
        });
        when(articleMetricsServiceMock.fetchWordCounts(any())).thenAnswer(invocation -> {
            Flux<ArticleName> names = invocation.getArgument(0);
            return names.map(name -> new WordCount(name.getName(), Integer.parseInt(name.getName()) * 100));
        });

        FluxExchangeResult<ArticleStatistics> result = testClient.get().uri(uB -> uB.pathSegment("statistics", "article").queryParam("updateInterval", 1000).build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(TEXT_EVENT_STREAM_UTF8)
                .returnResult(ArticleStatistics.class);

        StepVerifier.create(result.getResponseBody()
                .doOnNext(articleStatistics -> System.out.println("received on clientside: " + articleStatistics)))
                .expectNext(new ArticleStatistics(6, 250.0, 2.5, 100.0))
                .thenCancel()
                .verify();
    }

    @Test
    public void fetchArticleStatisticsWithShortUpdateInterval() {
        when(articleReadEventsServiceMock.readEvents()).thenReturn(Flux.interval(Duration.ofMillis(310)).take(4)
                .map(count -> createReadEvent(count, count.intValue())));
        when(articleMetricsServiceMock.fetchRatings(any())).thenAnswer(invocation -> {
            Flux<ArticleName> names = invocation.getArgument(0);
            return names
                    .doOnNext(name -> System.out.println("fetchRating: " + name))
                    .map(name -> new Rating(name.getName(), Integer.parseInt(name.getName())));
        });
        when(articleMetricsServiceMock.fetchWordCounts(any())).thenAnswer(invocation -> {
            Flux<ArticleName> names = invocation.getArgument(0);
            return names.map(name -> new WordCount(name.getName(), Integer.parseInt(name.getName()) * 100));
        });

        Flux<ArticleStatistics> result = statisticsController.fetchArticleStatistics(300)
                .doOnNext(articleStatistics -> System.out.println("received on clientside: " + articleStatistics));

        StepVerifier.create(result)
                .expectSubscription()
                .expectNext(new ArticleStatistics(1, 0.0, 0.0, 0.0))
                .expectNext(new ArticleStatistics(1, 100.0, 1.0, 1.0))
                .expectNext(new ArticleStatistics(1, 200.0, 2.0, 2.0))
                .expectNext(new ArticleStatistics(1, 300.0, 3.0, 3.0))
                .thenCancel()
                .verify();
    }

    @Test
    public void fetchTopArticleWithDefaultQueryParams() {
        when(articleReadEventsServiceMock.readEvents()).thenReturn(Flux.interval(Duration.ofMillis(245)).take(5)
                .flatMap(count -> Flux.just(createReadEvent(count, count.intValue())).repeat(count * 2)));

        FluxExchangeResult<TopArticle[]> result = testClient.get().uri("/top/article")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(TEXT_EVENT_STREAM_UTF8)
                .returnResult(TopArticle[].class);

        StepVerifier.create(result.getResponseBody().flatMap(Flux::fromArray))
                .expectNext(createTopArticle("3", 7))
                .expectNext(createTopArticle("2", 5))
                .expectNext(createTopArticle("1", 3))
                .thenCancel()
                .verify();
    }

    private TopArticle createTopArticle(String name, int reads) {
        return new TopArticle(name, ARTICLE_BASE_URL + "/article/" + name, reads);
    }

    private ArticleReadEvent createReadEvent(Long count, int fetchTimeInMillis) {
        return new ArticleReadEvent(count + "", fetchTimeInMillis);
    }

}
