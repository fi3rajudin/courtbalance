package com.fit.badminton.storage;

import java.net.*;
import java.net.http.*;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SupabaseQrStorageService implements QrStorageService {
    private final HttpClient client = HttpClient.newHttpClient();
    @Value("${app.storage.supabase-url:}")
    private String url;
    @Value("${app.storage.supabase-service-key:}")
    private String key;
    @Value("${app.storage.qr-bucket:member-qr}")
    private String bucket;

    public String upload(long memberId, byte[] bytes, String ct) {
        ensure();
        String ext = ct.toLowerCase(Locale.ROOT).contains("png") ? "png"
                : ct.toLowerCase(Locale.ROOT).contains("webp") ? "webp" : "jpg";
        String path = "member-" + memberId + "/qr." + ext;
        HttpRequest req = HttpRequest.newBuilder(URI.create(url + "/storage/v1/object/" + bucket + "/" + path))
                .header("Authorization", "Bearer " + key).header("apikey", key).header("Content-Type", ct)
                .header("x-upsert", "true").POST(HttpRequest.BodyPublishers.ofByteArray(bytes)).build();
        send(req);
        return path;
    }

    public QrPayload load(String path) {
        ensure();
        HttpRequest req = HttpRequest
                .newBuilder(URI.create(url + "/storage/v1/object/authenticated/" + bucket + "/" + path))
                .header("Authorization", "Bearer " + key).header("apikey", key).GET().build();
        try {
            HttpResponse<byte[]> r = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (r.statusCode() / 100 != 2)
                throw new IllegalStateException("Unable to load QR image");
            return new QrPayload(r.body(), r.headers().firstValue("content-type").orElse("image/png"));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load QR image", e);
        }
    }

    private void send(HttpRequest req) {
        try {
            HttpResponse<String> r = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() / 100 != 2)
                throw new IllegalStateException("QR upload failed: " + r.statusCode());
        } catch (Exception e) {
            throw new IllegalStateException("QR upload failed", e);
        }
    }

    private void ensure() {
        if (url == null || url.isBlank() || key == null || key.isBlank())
            throw new IllegalStateException("Supabase storage is not configured");
    }
}
