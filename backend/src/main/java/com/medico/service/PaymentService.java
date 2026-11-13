package com.medico.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

/** Razorpay via plain REST (no SDK). With no keys configured the app runs in demo-payment mode. */
@Service
public class PaymentService {
    private final RestClient http = RestClient.create();
    @Value("${razorpay.key-id:}") private String keyId;
    @Value("${razorpay.key-secret:}") private String keySecret;

    public boolean enabled() { return !keyId.isBlank() && !keySecret.isBlank(); }
    public String keyId() { return keyId; }

    @SuppressWarnings("unchecked")
    public String createOrder(int amountRupees, String receipt) {
        Map<String, Object> res = http.post().uri("https://api.razorpay.com/v1/orders")
                .headers(h -> h.setBasicAuth(keyId, keySecret)).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amountRupees * 100, "currency", "INR", "receipt", receipt))
                .retrieve().body(Map.class);
        return (String) res.get("id");
    }

    public boolean verify(String orderId, String paymentId, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { return false; }
    }

    public boolean refund(String paymentId) {
        if (paymentId == null || paymentId.startsWith("demo_")) return true;
        try {
            http.post().uri("https://api.razorpay.com/v1/payments/" + paymentId + "/refund")
                    .headers(h -> h.setBasicAuth(keyId, keySecret)).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of()).retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) { return false; }
    }
}
