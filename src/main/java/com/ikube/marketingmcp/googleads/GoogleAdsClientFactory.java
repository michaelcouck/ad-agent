package com.ikube.marketingmcp.googleads;

import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.auth.oauth2.UserCredentials;
import com.ikube.marketingmcp.config.GoogleAdsProperties;
import org.springframework.stereotype.Component;

@Component
public class GoogleAdsClientFactory {

    private final GoogleAdsProperties properties;

    public GoogleAdsClientFactory(GoogleAdsProperties properties) {
        this.properties = properties;
    }

    public GoogleAdsClient create() {
        require(properties.developerToken(), "GOOGLE_ADS_DEVELOPER_TOKEN");
        require(properties.clientId(), "GOOGLE_ADS_CLIENT_ID");
        require(properties.clientSecret(), "GOOGLE_ADS_CLIENT_SECRET");
        require(properties.refreshToken(), "GOOGLE_ADS_REFRESH_TOKEN");

        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(properties.clientId())
                .setClientSecret(properties.clientSecret())
                .setRefreshToken(properties.refreshToken())
                .build();

        GoogleAdsClient.Builder builder = GoogleAdsClient.newBuilder()
                .setDeveloperToken(properties.developerToken())
                .setCredentials(credentials);

        if (!blank(properties.loginCustomerId())) {
            builder.setLoginCustomerId(Long.parseLong(normalizeCustomerId(properties.loginCustomerId())));
        }
        return builder.build();
    }

    public String customerId() {
        require(properties.customerId(), "GOOGLE_ADS_CUSTOMER_ID");
        return normalizeCustomerId(properties.customerId());
    }

    private static String normalizeCustomerId(String customerId) {
        return customerId.replace("-", "").trim();
    }

    private static void require(String value, String name) {
        if (blank(value)) {
            throw new IllegalStateException(name + " is required");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
