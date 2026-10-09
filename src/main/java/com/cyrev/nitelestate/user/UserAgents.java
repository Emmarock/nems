package com.cyrev.nitelestate.user;

/**
 * Minimal, heuristic User-Agent summary for login-notification emails (see
 * UserService.notifyLogin) — just enough for the recipient to recognize "yes, that's my phone"
 * or "that's not me," not a precise device fingerprint.
 */
public final class UserAgents {

    private UserAgents() {
    }

    public static String describe(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown device";
        }
        String ua = userAgent.toLowerCase();
        String os = osOf(ua);
        String browser = browserOf(ua);
        if (os == null && browser == null) {
            return "Unknown device";
        }
        if (os == null) {
            return browser;
        }
        if (browser == null) {
            return os;
        }
        return browser + " on " + os;
    }

    private static String osOf(String ua) {
        if (ua.contains("iphone")) return "iPhone";
        if (ua.contains("ipad")) return "iPad";
        if (ua.contains("android")) return "Android";
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("mac os x") || ua.contains("macintosh")) return "macOS";
        if (ua.contains("linux")) return "Linux";
        return null;
    }

    private static String browserOf(String ua) {
        if (ua.contains("edg/")) return "Edge";
        if (ua.contains("crios/")) return "Chrome";
        if (ua.contains("chrome/")) return "Chrome";
        if (ua.contains("firefox/")) return "Firefox";
        if (ua.contains("safari/")) return "Safari";
        return null;
    }
}
