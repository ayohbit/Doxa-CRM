package com.doxa.crm.controller;

import com.doxa.crm.dto.webhook.LeadBrokerWebhookResponse;
import com.doxa.crm.service.LeadBrokerWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class LeadBrokerWebhookController {

    private final LeadBrokerWebhookService leadBrokerWebhookService;

    @PostMapping("/lead-broker")
    public ResponseEntity<LeadBrokerWebhookResponse> receiveLead(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Broker-Signature", required = false) String signature
    ) {
        LeadBrokerWebhookResponse response = leadBrokerWebhookService.handle(rawBody, signature);
        HttpStatus status = response.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }
}
