package com.thrive.ratelimit.ratelimitcache.Impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.thrive.client.RedisClient;
import com.thrive.client.cache.CacheName;
import com.thrive.ratelimit.ratelimitcache.RateLimitCache;
import com.thrive.constant.Constants;
import com.thrive.model.config.CacheConfig;
import com.thrive.model.config.CacheNameConfig;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapCache;

import java.util.Objects;
import java.util.Optional;

@Singleton
@Slf4j
public class RateLimitCacheImpl implements RateLimitCache {
    private final CacheNameConfig cacheNameConfig;
    private final RMapCache<String, Integer> limitValueMapCache;
    @Inject
    public RateLimitCacheImpl(RedisClient redisClient, CacheConfig cacheConfig) {
        this.cacheNameConfig = cacheConfig.getMapConfig().getOrDefault(CacheName.RATE_LIMIT, Constants.DEFAULT_RATE_LIMIT_CONFIG);
        this.limitValueMapCache = redisClient.getClient().getMapCache(CacheName.RATE_LIMIT.name());
    }

    @Override
    public Optional<Integer> get(String cacheKey) {
        Integer count = limitValueMapCache.getOrDefault(cacheKey, null);
        if(Objects.nonNull(count) && count != -1) {
            log.info("TTL left {}", Math.abs(limitValueMapCache.remainTimeToLive(cacheKey))/1000);
            return Optional.of(count);
        }
        return Optional.empty();
    }

    @Override
    public void put(String cacheKey, long ttl) {
        Optional<Integer> currCount = get(cacheKey);
        if(!currCount.isEmpty())
            // it returns time in millis
            ttl = Math.abs(limitValueMapCache.remainTimeToLive(cacheKey))/1000;
        log.info("new TTL  {}  {}", ttl, cacheNameConfig.getTtlType().name());
        limitValueMapCache.put(cacheKey, currCount.orElse(0) + Integer.valueOf(1), ttl, cacheNameConfig.getTtlType());
    }
}
