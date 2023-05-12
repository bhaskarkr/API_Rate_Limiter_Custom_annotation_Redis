package com.thrive.ratelimit;

import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

@Slf4j
@Provider
public class RateLimitServiceInitializer implements DynamicFeature {

    @Inject
    private Injector injector;



    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        if(resourceInfo.getResourceMethod().getAnnotation(RateLimit.class) != null) {
            RateLimitService rateLimitService = injector.getInstance(RateLimitService.class);
            rateLimitService.setResourceInfo(resourceInfo);
            context.register(rateLimitService);
        }
    }
}
