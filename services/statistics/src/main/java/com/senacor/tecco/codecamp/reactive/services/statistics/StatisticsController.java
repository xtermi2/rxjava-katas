package com.senacor.tecco.codecamp.reactive.services.statistics;

import com.senacor.tecco.codecamp.reactive.services.statistics.external.ArticleReadEventsService;
import com.senacor.tecco.codecamp.reactive.services.statistics.external.ArticleMetricsService;
import com.senacor.tecco.codecamp.reactive.services.statistics.model.ArticleMetrics;
import com.senacor.tecco.codecamp.reactive.services.statistics.external.ArticleReadEvent;
import com.senacor.tecco.codecamp.reactive.services.statistics.model.ArticleStatistics;
import com.senacor.tecco.codecamp.reactive.services.statistics.model.TopArticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

/**
 * @author Andri Bremm
 */
@RestController
public class StatisticsController {

    private ArticleReadEventsService articleReadEventsService;
    private ArticleMetricsService articleMetricsService;

    @Autowired
    public StatisticsController(ArticleReadEventsService articleReadEventsService, ArticleMetricsService articleMetricsService) {
        this.articleReadEventsService = articleReadEventsService;
        this.articleMetricsService = articleMetricsService;
    }

    private static final Map<String, Long> readStatistics = new ConcurrentHashMap<>();

    @GetMapping(value = "/top/article", produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<List<TopArticle>> fetchArticleStatistics(@RequestParam(required = false, defaultValue = "1000") int updateInterval,
                                                         @RequestParam(required = false, defaultValue = "5") int numberOfTopArticles) {
        return articleReadEventsService.readEvents()
                .doOnNext(StatisticsController::updateReadStatistic)
                .sampleMillis(updateInterval)
                .map(readEvent -> createTopArticleList(numberOfTopArticles))
                .log();
    }

    @GetMapping(value = "/statistics/article", produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<ArticleStatistics> fetchArticleStatistics(@RequestParam(required = false, defaultValue = "1000") int updateInterval) {
        return articleReadEventsService.readEvents()
                .map(articleReadEvent -> articleReadEvent.getArticleName())
                .flatMap(articleName -> Flux.zip(
                        articleMetricsService.fetchRating(articleName),
                        articleMetricsService.fetchWordCount(articleName),
                        (rating, wordCount) -> new ArticleMetrics(rating, wordCount)))
                .bufferMillis(updateInterval)
                .map(StatisticsController::calculateArticleStatistics)
                .log();
    }

    private List<TopArticle> createTopArticleList(int numberOfTopArticles) {
        return readStatistics.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String,Long>>comparingLong(Map.Entry::getValue).reversed())
                .limit(numberOfTopArticles)
                // TODO improve url creation
                .map(entry -> new TopArticle(entry.getKey(), "http://localhost:8081/article/"+entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private static void updateReadStatistic(ArticleReadEvent readEvent) {
        long reads = 1l;
        String articleName = readEvent.getArticleName();
        if (readStatistics.containsKey(articleName)) {
            reads = readStatistics.get(articleName) + 1;
        }
        readStatistics.put(articleName, reads);
    }

    private static ArticleStatistics calculateArticleStatistics(List<ArticleMetrics> articleMetricss) {
        long wordCountSum = 0;
        long ratingSum = 0;
        for (ArticleMetrics metrics : articleMetricss) {
            wordCountSum += metrics.getWordCount();
            ratingSum += metrics.getRating();
        }
        Integer numOfArticles = articleMetricss.size();
        double wordCountAvg = wordCountSum / numOfArticles.doubleValue();
        double ratingAvg = ratingSum / numOfArticles.doubleValue();
        return new ArticleStatistics(numOfArticles, wordCountAvg, ratingAvg);
    }

}