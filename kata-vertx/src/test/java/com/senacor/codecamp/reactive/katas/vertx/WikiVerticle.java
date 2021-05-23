package com.senacor.codecamp.reactive.katas.vertx;

import com.senacor.codecamp.reactive.services.WikiService;
import com.senacor.codecamp.reactive.util.DelayFunction;
import com.senacor.codecamp.reactive.util.FlakinessFunction;
import io.vertx.core.AbstractVerticle;

/**
 * @author Andreas Keefer
 */
public class WikiVerticle extends AbstractVerticle {

    private final WikiService wikiService = WikiService.create(
            DelayFunction.withNoDelay(),
            FlakinessFunction.noFlakiness(),
            false,
            "de");

    @Override
    public void start() throws Exception {
        vertx.eventBus().<String>consumer("fetchArticle").handler(msg ->
                wikiService.fetchArticleObservable(msg.body())
                        .subscribe(msg::reply)
        );
    }
}
