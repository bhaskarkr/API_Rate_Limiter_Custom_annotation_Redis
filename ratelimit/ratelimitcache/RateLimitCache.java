package com.thrive.ratelimit.ratelimitcache;


import java.util.Optional;

public interface RateLimitCache {
    Optional<Integer> get(String UserId);
    void put(String userId, long ttl);
}
