package com.ikube.marketingmcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "marketing.google-ads")
public record GoogleAdsProperties(
        String developerToken,
        String clientId,
        String clientSecret,
        String refreshToken,
        String customerId,
        String loginCustomerId) {
}
