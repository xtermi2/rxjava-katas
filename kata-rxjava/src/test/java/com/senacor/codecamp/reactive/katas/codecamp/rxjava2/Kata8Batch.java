package com.senacor.codecamp.reactive.katas.codecamp.rxjava2;

import com.senacor.codecamp.reactive.katas.KataClassification;
import com.senacor.codecamp.reactive.services.PersistService;
import com.senacor.codecamp.reactive.services.WikiService;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * @author Andreas Keefer
 */
public class Kata8Batch {

    private final WikiService wikiService = WikiService.create();
    private final PersistService persistService = PersistService.create();

    @Test
    @KataClassification(KataClassification.Classification.mandatory)
    public void withoutBatch() throws Exception {
        // 1. use WikiService#wikiArticleBeingReadObservableBurst that returns a stream of wiki article being read
        // 2. watch the stream 2 sec
        // 3. save the article (PersistService.save(String)). The service returns the execution time
        // 4. sum the execution time of the service calls and print the result

        wikiService.wikiArticleBeingReadObservableBurst()
        	.take(2, TimeUnit.SECONDS)
        	.map(article -> persistService.save(article))
//        	.doOnNext(t -> System.out.println("time: " + t))
        	.reduce(Long::sum)
        	.doOnSuccess(r -> System.out.println("result " + r))
        	.test()
        	.awaitDone(5, TimeUnit.SECONDS)
        	.assertValueCount(1)
        	.assertComplete();
    }


    @Test
    @KataClassification(KataClassification.Classification.mandatory)
    public void batch() throws Exception {
        // 1. do the same as above, but this time use the method #save(Iterable) to save a batch of articles.
        //    use a batch size of 5.
        //    Please note that this is a stream - you can not wait until all articles are delivered to save everything in a batch

        wikiService.wikiArticleBeingReadObservableBurst()
        	.buffer(5)
	    	.take(2, TimeUnit.SECONDS)
	    	.map(persistService::save)
	//    	.doOnNext(t -> System.out.println("time: " + t))
	    	.reduce(Long::sum)
	    	.doOnSuccess(r -> System.out.println("result " + r))
	    	.test()
	    	.awaitDone(5, TimeUnit.SECONDS)
	    	.assertValueCount(1)
	    	.assertComplete();
    }

    @Test
    @KataClassification(KataClassification.Classification.advanced)
    public void batch2() throws Exception {
        // 2. If the batch size is not reached within 500 milliseconds,
        //    flush the buffer anyway by writing to the service

        wikiService.wikiArticleBeingReadObservableBurst()
    	.take(2, TimeUnit.SECONDS)
    	.buffer(5, 500, TimeUnit.MILLISECONDS)
    	.map(persistService::save)
//    	.doOnNext(t -> System.out.println("time: " + t))
    	.reduce(Long::sum)
    	.doOnSuccess(r -> System.out.println("result " + r))
    	.test()
    	.awaitDone(5, TimeUnit.SECONDS)
    	.assertValueCount(1)
    	.assertComplete();
        
        
        
    }
}
