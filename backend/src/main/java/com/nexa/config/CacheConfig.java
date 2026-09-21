package com.nexa.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Active @Cacheable / @CacheEvict dans l'application. En production (profil "prod"),
 * spring.cache.type=redis fait passer ce cache par Redis, partagé entre toutes les
 * instances du backend. En local, Spring retombe sur un cache mémoire simple,
 * suffisant pour développer sans lancer Redis.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
