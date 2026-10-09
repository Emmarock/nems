package com.cyrev.nitelestate.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * Best-effort IP -> "City, Country" lookup for login-notification emails, via ipapi.co's free,
 * keyless endpoint. Never throws - any failure (private/local IP, network error, rate limit,
 * malformed response) just means the email omits the Location line. This must never slow down or
 * break login itself - see UserService.notifyLogin, which runs this off the request thread.
 */
@Slf4j
@Service
public class GeoLocationService {

    private final RestClient restClient;

    public GeoLocationService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(3_000);
        this.restClient = RestClient.builder()
                .baseUrl("https://ipapi.co")
                .requestFactory(factory)
                .build();
    }

    public String lookup(String ip) {
        if (ip == null || isLocal(ip)) {
            return null;
        }
        try {
            Map<?, ?> json = restClient.get().uri("/{ip}/json/", ip).retrieve().body(Map.class);
            if (json == null || Boolean.TRUE.equals(json.get("error"))) {
                return null;
            }
            Object city = json.get("city");
            Object country = json.get("country_name");
            if (city != null && country != null) {
                return city + ", " + country;
            }
            return city != null ? String.valueOf(city) : (country != null ? String.valueOf(country) : null);
        } catch (Exception ex) {
            log.debug("Geo-IP lookup failed for {}: {}", ip, ex.getMessage());
            return null;
        }
    }

    /** Covers loopback/private/link-local ranges (10/8, 172.16/12, 192.168/16, ::1, etc.) via the JDK's own classification. */
    private boolean isLocal(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress() || addr.isAnyLocalAddress();
        } catch (UnknownHostException ex) {
            return true;
        }
    }
}
