package com.Hardik.projects.AirbnbApp.controller;

import com.Hardik.projects.AirbnbApp.service.BookingService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final BookingService bookingService;

    @Value("${stripe.webhook.secret}")
    private String endPointSecret;

    @PostMapping("/payments")
    public ResponseEntity<Void>  capturePayments(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader){
        try {
            Event event= Webhook.constructEvent(payload,sigHeader,endPointSecret);
            bookingService.capturePayments(event);
            return ResponseEntity.noContent().build();
        }catch (SignatureVerificationException e){
            throw new RuntimeException(e);
        }
    }
}

