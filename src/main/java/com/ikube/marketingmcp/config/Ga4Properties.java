package com.ikube.marketingmcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "marketing.ga4")
public record Ga4Properties(String propertyId) {
}
