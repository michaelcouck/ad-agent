package com.ikube.marketingmcp.ga4;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.analytics.data.v1beta.BetaAnalyticsDataClient;
import com.google.analytics.data.v1beta.DateRange;
import com.google.analytics.data.v1beta.Dimension;
import com.google.analytics.data.v1beta.Metric;
import com.google.analytics.data.v1beta.Row;
import com.google.analytics.data.v1beta.RunReportRequest;
import com.google.analytics.data.v1beta.RunReportResponse;
import com.ikube.marketingmcp.config.Ga4Properties;
import org.springframework.stereotype.Service;

@Service
public class Ga4ReportingService {

    private static final int MAX_LIMIT = 500;

    private final Ga4Properties properties;

    public Ga4ReportingService(Ga4Properties properties) {
        this.properties = properties;
    }

    public List<Map<String, Object>> trafficAcquisition(String since, String until, int limit) {
        return runReport(
                since,
                until,
                List.of("sessionSourceMedium", "sessionCampaignName", "country", "deviceCategory"),
                List.of("sessions", "engagedSessions", "conversions", "totalRevenue"),
                limit);
    }

    public List<Map<String, Object>> landingPages(String since, String until, int limit) {
        return runReport(
                since,
                until,
                List.of("landingPagePlusQueryString", "sessionSourceMedium", "sessionCampaignName", "country"),
                List.of("sessions", "engagedSessions", "conversions", "averageSessionDuration"),
                limit);
    }

    public List<Map<String, Object>> events(String since, String until, int limit) {
        return runReport(
                since,
                until,
                List.of("eventName", "sessionSourceMedium", "sessionCampaignName", "country"),
                List.of("eventCount", "conversions", "totalUsers"),
                limit);
    }

    public List<Map<String, Object>> runReport(
            String since,
            String until,
            List<String> dimensions,
            List<String> metrics,
            int limit) {
        require(properties.propertyId(), "GA4_PROPERTY_ID");
        try (BetaAnalyticsDataClient client = BetaAnalyticsDataClient.create()) {
            RunReportRequest request = RunReportRequest.newBuilder()
                    .setProperty("properties/" + properties.propertyId())
                    .addDateRanges(DateRange.newBuilder().setStartDate(since).setEndDate(until).build())
                    .addAllDimensions(dimensions.stream().map(name -> Dimension.newBuilder().setName(name).build()).toList())
                    .addAllMetrics(metrics.stream().map(name -> Metric.newBuilder().setName(name).build()).toList())
                    .setLimit(clampLimit(limit))
                    .build();
            RunReportResponse response = client.runReport(request);
            return toRows(response);
        } catch (Exception e) {
            throw new IllegalStateException("GA4 report failed", e);
        }
    }

    private static List<Map<String, Object>> toRows(RunReportResponse response) {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> dimensions = response.getDimensionHeadersList().stream().map(header -> header.getName()).toList();
        List<String> metrics = response.getMetricHeadersList().stream().map(header -> header.getName()).toList();
        for (Row row : response.getRowsList()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < dimensions.size(); i++) {
                map.put(dimensions.get(i), row.getDimensionValues(i).getValue());
            }
            for (int i = 0; i < metrics.size(); i++) {
                map.put(metrics.get(i), row.getMetricValues(i).getValue());
            }
            rows.add(map);
        }
        return rows;
    }

    private static int clampLimit(int limit) {
        if (limit <= 0) {
            return 100;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static void require(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(name + " is required");
        }
    }
}
