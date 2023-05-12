package com.thrive.ratelimit;


import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.thrive.ratelimit.ratelimitcache.RateLimitCache;
import com.thrive.core.ErrorCode;
import com.thrive.core.UserException;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import static com.thrive.ratelimit.RateLimitType.GET_USER;

@Slf4j
@RateLimit
//@Priority(Priorities.USER)
public class RateLimitService implements ContainerRequestFilter {
    private static final String END_USER_TOKEN = "end-user-token";
    // Add this to configuration file
    private static final Map<RateLimitType, RateLimitConfig> TYPE_TO_LIMIT = ImmutableMap.of(GET_USER, RateLimitConfig.builder().duration(10).unit(RateLimitUnit.SECOND).maxLimit(5).build());

    private ResourceInfo resourceInfo;
    @Inject
    private RateLimitCache rateLimitCache;

    public void setResourceInfo(ResourceInfo resourceInfo) {
        this.resourceInfo = resourceInfo;
    }

    private Integer getTimeInSeconds(RateLimitConfig rateLimitConfig) {
        if(rateLimitConfig.getUnit() == RateLimitUnit.HOUR)
            return rateLimitConfig.getDuration() * 3600;
        else if(rateLimitConfig.getUnit() == RateLimitUnit.MINUTE)
            return rateLimitConfig.getDuration() * 60;
        return rateLimitConfig.getDuration();
    }

    private String getCacheKey(String userId, RateLimitType rateLimitType) {
        return userId + "_" + rateLimitType.name();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Method method = resourceInfo.getResourceMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if(Objects.nonNull(rateLimit) && Objects.nonNull(rateLimit.rateLimitType()) && TYPE_TO_LIMIT.containsKey(rateLimit.rateLimitType())) {
            if (!requestContext.getHeaders().containsKey(END_USER_TOKEN)) {
                throw UserException.error(ErrorCode.USER_TOKEN_MISSING, "Please include 'end-user-token' in the header");
            }
            RateLimitConfig limitConfig = TYPE_TO_LIMIT.get(rateLimit.rateLimitType());
            // Extract userId from the token, for simplicity using user token as userID
            String userId = requestContext.getHeaders().get(END_USER_TOKEN).stream().findFirst().get();
            int current_count = rateLimitCache.get(getCacheKey(userId, rateLimit.rateLimitType())).orElse(0);
            if(current_count > limitConfig.getMaxLimit()) {
                throw UserException.error(ErrorCode.RATE_LIMIT_EXCEED, "Please try after sometime");
            }
            rateLimitCache.put(getCacheKey(userId, rateLimit.rateLimitType()), getTimeInSeconds(limitConfig));
        }
    }
}
