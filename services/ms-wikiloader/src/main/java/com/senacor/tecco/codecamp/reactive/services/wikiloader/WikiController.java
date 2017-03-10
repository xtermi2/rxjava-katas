package com.senacor.tecco.codecamp.reactive.services.wikiloader;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.base.Stopwatch;
import com.senacor.tecco.codecamp.reactive.services.wikiloader.model.Article;
import com.senacor.tecco.codecamp.reactive.services.wikiloader.model.Article.NameOnly;
import com.senacor.tecco.codecamp.reactive.services.wikiloader.model.ArticleName;
import com.senacor.tecco.codecamp.reactive.services.wikiloader.model.Rating;
import com.senacor.tecco.codecamp.reactive.services.wikiloader.model.WordCount;
import com.senacor.tecco.reactive.services.CountService;
import com.senacor.tecco.reactive.services.RatingService;
import com.senacor.tecco.reactive.services.WikiService;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Andreas Keefer
 */
@RestController
@RequestMapping("/article")
public class WikiController {

    public static final int BUFFER_READ_EVENTS = 250;

    private final DirectProcessor<Article> readArticles = DirectProcessor.create();

    private final WikiService wikiService;
    private final CountService countService;
    private final RatingService ratingService;
    private final PublisherCache<String, Article> cache;

    @Autowired
    public WikiController(WikiService wikiService, CountService countService, RatingService ratingService,
                          @Value("${article.cache.size}") int cacheSize) {
        this.wikiService = wikiService;
        this.countService = countService;
        this.ratingService = ratingService;
        this.cache = new PublisherCache<>(this::getArticle, cacheSize);
    }

    @GetMapping("/{name}")
    public Mono<Article> fetchArticle(@PathVariable String name) {
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        return cache.lookup(name)
                .doOnSubscribe(subscription -> stopwatch.start())
                .map(article -> Article.newBuilder(article)
                        .withFetchTimeInMillis((int) stopwatch.stop().elapsed(TimeUnit.MILLISECONDS))
                        .build())
                .doOnNext(readArticles::onNext);
    }

    private Mono<Article> getArticle(String name) {
        return wikiService.fetchArticleNonBlocking(name)
                .map(content -> Article.newBuilder().withName(name).withContent(content).build());
    }

    @CrossOrigin
    @GetMapping("/readevents")
    @JsonView(NameOnly.class)
    public Flux<List<Article>> getReadStream() {
        return readArticles
                .bufferMillis(BUFFER_READ_EVENTS)
                .filter(list -> !list.isEmpty());
    }

    private Mono<ParsedPage> getParsedArticle(String name) {
        return cache.lookup(name)
                .map(Article::getContent)
                .map(wikiService::parseMediaWikiText);
    }

    @GetMapping("/{name}/wordcount")
    public Mono<Integer> getWordCount(@PathVariable String name) {
        return getParsedArticle(name)
                .map(countService::countWords);
    }

    @RequestMapping("/wordcounts")
    public Flux<WordCount> countWords(@RequestBody Flux<ArticleName> names) {
        return names
                .flatMap(articleName -> getParsedArticle(articleName.getName())
                        .map(countService::countWords)
                        .map(count -> new WordCount(articleName.getName(), count)))
                .log();
    }

    @GetMapping("/{name}/rating")
    public Mono<Integer> getRating(@PathVariable String name) {
        return getParsedArticle(name)
                .map(ratingService::rate);
    }

    @RequestMapping("/ratings")
    public Flux<Rating> ratings(@RequestBody Flux<ArticleName> names) {
        return names
                .flatMap(articleName -> getParsedArticle(articleName.getName())
                        .map(ratingService::rate)
                        .map(rating -> new Rating(articleName.getName(), rating)));
    }
}
