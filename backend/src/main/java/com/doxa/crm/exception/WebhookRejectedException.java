package com.doxa.crm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class WebhookRejectedException extends RuntimeException {

    public WebhookRejectedException(String message) {
        super(message);
    }
}
