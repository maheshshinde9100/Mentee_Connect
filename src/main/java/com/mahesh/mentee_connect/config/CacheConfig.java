package com.mahesh.mentee_connect.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Configure Caffeine cache specifications
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES) // cache entries expire 5 minutes after creation
                .initialCapacity(100)
                .maximumSize(500));
                
        // Define the cache zones we'll use in services
        cacheManager.setCacheNames(Arrays.asList("analytics", "students", "mentors", "batches"));
        
        return cacheManager;
    }
}
