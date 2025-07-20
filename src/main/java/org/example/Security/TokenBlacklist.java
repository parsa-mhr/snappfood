package org.example.Security;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TokenBlacklist {
    private static final Map<String, Date> blacklist = new HashMap<>();

    public static void add(String jti, Date expiry) {
        blacklist.put(jti, expiry);
    }

    public static boolean isBlacklisted(String jti) {
        Date expiry = blacklist.get(jti);
        return expiry != null && expiry.after(new Date());
    }
}
