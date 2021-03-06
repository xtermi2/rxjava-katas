package com.senacor.codecamp.reactive.services;

import com.google.common.collect.Iterables;
import com.senacor.codecamp.reactive.services.integration.BlackHoleDatabase;
import com.senacor.codecamp.reactive.services.integration.BlackHoleDatabaseImpl;
import com.senacor.codecamp.reactive.util.*;

public class PersistService {

    private final BlackHoleDatabase database;

    public static PersistService create() {
        return create(DelayFunction.withNoDelay(),
                FlakinessFunction.noFlakiness());
    }

    public static PersistService create(DelayFunction delayFunction) {
        return create(delayFunction, FlakinessFunction.noFlakiness());
    }

    public static PersistService create(FlakinessFunction flakinessFunction) {
        return create(DelayFunction.withNoDelay(), flakinessFunction);
    }

    public static PersistService create(DelayFunction delayFunction,
                                        FlakinessFunction flakinessFunction) {
        return new PersistService(flakinessFunction, delayFunction);
    }

    private PersistService(FlakinessFunction flakinessFunction, DelayFunction delayFunction) {
        database = StopWatchProxy.newJdkProxy(
                DelayProxy.newJdkProxy(
                        FlakyProxy.newJdkProxy(new BlackHoleDatabaseImpl(), flakinessFunction)
                        , delayFunction));
    }

    /**
     * @param wikiArticle article
     * @return runtime
     */
    public long save(String wikiArticle) {
        return database.saveOne(wikiArticle);
    }

    /**
     * @param wikiArticle article
     * @return runtime
     */
    public long save(Iterable<String> wikiArticle) {
        if (Iterables.isEmpty(wikiArticle)) {
            return 0;
        }
        return database.saveBatch(wikiArticle);
    }


}
