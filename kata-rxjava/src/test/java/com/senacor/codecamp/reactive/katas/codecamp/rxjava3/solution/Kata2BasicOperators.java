package com.senacor.codecamp.reactive.katas.codecamp.rxjava3.solution;

import com.senacor.codecamp.reactive.services.WikiService;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.senacor.codecamp.reactive.util.ReactiveUtil.print;

/**
 * @author Andreas Keefer
 */
public class Kata2BasicOperators {

    private final WikiService wikiService = WikiService.create();

    @Test
    public void basicsA() throws Exception {
        // 1. Use the WikiService (fetchArticleObservable) and fetch an arbitrary wikipedia article
        // 2. transform the result with the WikiService#parseMediaWikiText to an object structure
        //    and print out the first Paragraph

        wikiService.fetchArticleObservable("Physik")
                //.doOnNext(debug -> print("fetchArticleObservable res: %s", debug))
                .map(wikiService::parseMediaWikiText)
                .map(parsedPage -> parsedPage.getParagraph(0).toString())
                .doOnNext(next -> print("1st Paragraph: %s", next))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertValue(value -> value.contains("Dieser_Artikel, beschreibt die Naturwissenschaft Physik."))
                .assertComplete();
    }

    @Test
    public void basicsB() throws Exception {
        // 3. split the Article (ParsedPage.getText()) in words (separator=" ")
        // 4. sum the number of letters of all words beginning with character 'a' to the console

        wikiService.fetchArticleObservable("Physik")
                .map(wikiService::parseMediaWikiText)
                .flatMapIterable(parsedPage -> Arrays.asList(StringUtils.split(parsedPage.getText(), " ")))
                //.flatMap(parsedPage -> Observable.from(StringUtils.split(parsedPage.getText(), " ")))
                .filter(word -> word.startsWith("a"))
                .doOnNext(next -> print("words starting with 'a': %s", next))
                .reduce(0, (letterCount, word) -> letterCount + word.length())
                .doOnSuccess(next -> print("letter count of 'a'-words: %s", next))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertValue(value -> value > 1000)
                .assertComplete();
    }

    @Test
    public void basicsC() throws Exception {
        // 5. filter out redundant words beginning with 'a'
        // 6. order them by length and take only the top 10 words in length

        wikiService.fetchArticleObservable("Physik")
                .map(wikiService::parseMediaWikiText)
                .flatMapIterable(parsedPage -> Arrays.asList(StringUtils.split(parsedPage.getText(), " ")))
                .filter(word -> word.startsWith("a"))
                .distinct()
                .sorted((o1, o2) -> Integer.compare(o2.length(), o1.length()))
                .take(10)
                .doOnNext(next -> print("Top 10 words starting with 'a': %s", next))
                .reduce(0, (letterCount, word) -> letterCount + word.length())
                .doOnSuccess(next -> print("letter count of 'a'-words: %s", next))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertValue(value -> value > 100)
                .assertComplete();
    }
}
