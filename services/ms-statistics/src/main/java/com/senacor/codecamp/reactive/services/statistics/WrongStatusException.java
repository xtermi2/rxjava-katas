package com.senacor.codecamp.reactive.services.statistics;

import org.springframework.web.reactive.function.client.ClientResponse;

import java.util.function.Consumer;

import static org.springframework.http.HttpStatus.OK;

/**
 * @author Daniel Heinrich
 */
public class WrongStatusException extends RuntimeException {

    public WrongStatusException(ClientResponse reponse) {
        super(reponse.statusCode() + ": " + reponse.statusCode().getReasonPhrase());
    }

    public static Consumer<ClientResponse> okFilter() {
        return r -> {
            if (r.statusCode() != OK) {
                throw new WrongStatusException(r);
            }
        };
    }
}
