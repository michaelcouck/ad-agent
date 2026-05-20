package com.ikube.marketingmcp.googleads;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v23.common.KeywordInfo;
import com.google.ads.googleads.v23.enums.CampaignStatusEnum.CampaignStatus;
import com.google.ads.googleads.v23.enums.KeywordMatchTypeEnum.KeywordMatchType;
import com.google.ads.googleads.v23.resources.Campaign;
import com.google.ads.googleads.v23.resources.CampaignBudget;
import com.google.ads.googleads.v23.resources.CampaignCriterion;
import com.google.ads.googleads.v23.services.CampaignBudgetOperation;
import com.google.ads.googleads.v23.services.CampaignBudgetServiceClient;
import com.google.ads.googleads.v23.services.CampaignCriterionOperation;
import com.google.ads.googleads.v23.services.CampaignCriterionServiceClient;
import com.google.ads.googleads.v23.services.CampaignOperation;
import com.google.ads.googleads.v23.services.CampaignServiceClient;
import com.google.ads.googleads.v23.services.GoogleAdsRow;
import com.google.ads.googleads.v23.services.GoogleAdsServiceClient;
import com.google.ads.googleads.v23.services.MutateCampaignBudgetsResponse;
import com.google.ads.googleads.v23.services.MutateCampaignCriteriaResponse;
import com.google.ads.googleads.v23.services.MutateCampaignsResponse;
import com.google.ads.googleads.v23.services.SearchGoogleAdsRequest;
import com.google.ads.googleads.v23.utils.ResourceNames;
import com.google.protobuf.FieldMask;
import com.google.protobuf.util.FieldMaskUtil;
import com.ikube.marketingmcp.guard.ChangeGuard;
import com.ikube.marketingmcp.guard.ChangeLogService;
import com.ikube.marketingmcp.guard.GuardDecision;
import org.springframework.stereotype.Service;

@Service
public class GoogleAdsReportingService {

    private static final int MAX_LIMIT = 500;

    private final GoogleAdsClientFactory clientFactory;
    private final ChangeGuard changeGuard;
    private final ChangeLogService changeLogService;

    public GoogleAdsReportingService(
            GoogleAdsClientFactory clientFactory,
            ChangeGuard changeGuard,
            ChangeLogService changeLogService) {
        this.clientFactory = clientFactory;
        this.changeGuard = changeGuard;
        this.changeLogService = changeLogService;
    }

    public List<Map<String, Object>> campaignPerformance(String since, String until, int limit) {
        String query = """
                SELECT
                  campaign.id,
                  campaign.name,
                  campaign.status,
                  campaign_budget.amount_micros,
                  metrics.impressions,
                  metrics.clicks,
                  metrics.cost_micros,
                  metrics.conversions,
                  metrics.conversions_value
                FROM campaign
                WHERE segments.date BETWEEN '%s' AND '%s'
                ORDER BY metrics.cost_micros DESC
                LIMIT %d
                """.formatted(since, until, clampLimit(limit));
        return runGaql(query, clampLimit(limit));
    }

    public List<Map<String, Object>> countryPerformance(String since, String until, int limit) {
        String query = """
                SELECT
                  campaign.id,
                  campaign.name,
                  geographic_view.country_criterion_id,
                  metrics.impressions,
                  metrics.clicks,
                  metrics.cost_micros,
                  metrics.conversions
                FROM geographic_view
                WHERE segments.date BETWEEN '%s' AND '%s'
                ORDER BY metrics.conversions DESC
                LIMIT %d
                """.formatted(since, until, clampLimit(limit));
        return runGaql(query, clampLimit(limit));
    }

    public List<Map<String, Object>> searchTerms(String since, String until, int limit) {
        String query = """
                SELECT
                  campaign.id,
                  campaign.name,
                  ad_group.id,
                  ad_group.name,
                  search_term_view.search_term,
                  metrics.impressions,
                  metrics.clicks,
                  metrics.cost_micros,
                  metrics.conversions
                FROM search_term_view
                WHERE segments.date BETWEEN '%s' AND '%s'
                ORDER BY metrics.cost_micros DESC
                LIMIT %d
                """.formatted(since, until, clampLimit(limit));
        return runGaql(query, clampLimit(limit));
    }

    public List<Map<String, Object>> runGaql(String query, int limit) {
        List<Map<String, Object>> rows = new ArrayList<>();
        GoogleAdsClient client = clientFactory.create();
        try (GoogleAdsServiceClient service = client.getLatestVersion().createGoogleAdsServiceClient()) {
            SearchGoogleAdsRequest request = SearchGoogleAdsRequest.newBuilder()
                    .setCustomerId(clientFactory.customerId())
                    .setQuery(query)
                    .build();
            int count = 0;
            for (GoogleAdsRow row : service.search(request).iterateAll()) {
                rows.add(rowToMap(row));
                count++;
                if (count >= clampLimit(limit)) {
                    break;
                }
            }
        }
        return rows;
    }

    public Map<String, Object> setCampaignStatus(long campaignId, CampaignStatus status, String confirmationToken) {
        GuardDecision decision = changeGuard.checkWrite("set_campaign_status", confirmationToken);
        Map<String, Object> payload = Map.of("campaignId", campaignId, "status", status.name());
        changeLogService.log("set_campaign_status", decision, payload);
        if (!decision.allowed()) {
            return Map.of("decision", decision, "request", payload);
        }

        GoogleAdsClient client = clientFactory.create();
        String customerId = clientFactory.customerId();
        Campaign campaign = Campaign.newBuilder()
                .setResourceName(ResourceNames.campaign(Long.parseLong(customerId), campaignId))
                .setStatus(status)
                .build();
        CampaignOperation operation = CampaignOperation.newBuilder()
                .setUpdate(campaign)
                .setUpdateMask(FieldMaskUtil.fromString("status"))
                .build();
        try (CampaignServiceClient service = client.getLatestVersion().createCampaignServiceClient()) {
            MutateCampaignsResponse response = service.mutateCampaigns(customerId, List.of(operation));
            return Map.of("decision", decision, "response", response.toString());
        }
    }

    public Map<String, Object> updateCampaignBudgetMicros(
            long campaignBudgetId,
            long currentBudgetMicros,
            long proposedBudgetMicros,
            String confirmationToken) {
        GuardDecision decision = changeGuard.checkBudgetChange(currentBudgetMicros, proposedBudgetMicros, confirmationToken);
        Map<String, Object> payload = Map.of(
                "campaignBudgetId", campaignBudgetId,
                "currentBudgetMicros", currentBudgetMicros,
                "proposedBudgetMicros", proposedBudgetMicros);
        changeLogService.log("update_campaign_budget", decision, payload);
        if (!decision.allowed()) {
            return Map.of("decision", decision, "request", payload);
        }

        GoogleAdsClient client = clientFactory.create();
        String customerId = clientFactory.customerId();
        CampaignBudget budget = CampaignBudget.newBuilder()
                .setResourceName(ResourceNames.campaignBudget(Long.parseLong(customerId), campaignBudgetId))
                .setAmountMicros(proposedBudgetMicros)
                .build();
        FieldMask mask = FieldMaskUtil.fromString("amount_micros");
        CampaignBudgetOperation operation = CampaignBudgetOperation.newBuilder()
                .setUpdate(budget)
                .setUpdateMask(mask)
                .build();
        try (CampaignBudgetServiceClient service = client.getLatestVersion().createCampaignBudgetServiceClient()) {
            MutateCampaignBudgetsResponse response = service.mutateCampaignBudgets(customerId, List.of(operation));
            return Map.of("decision", decision, "response", response.toString());
        }
    }

    public Map<String, Object> addCampaignNegativeKeywords(
            long campaignId,
            List<String> keywords,
            KeywordMatchType matchType,
            String confirmationToken) {
        GuardDecision decision = changeGuard.checkWrite("add_campaign_negative_keywords", confirmationToken);
        Map<String, Object> payload = Map.of("campaignId", campaignId, "keywords", keywords, "matchType", matchType.name());
        changeLogService.log("add_campaign_negative_keywords", decision, payload);
        if (!decision.allowed()) {
            return Map.of("decision", decision, "request", payload);
        }

        GoogleAdsClient client = clientFactory.create();
        String customerId = clientFactory.customerId();
        long numericCustomerId = Long.parseLong(customerId);
        List<CampaignCriterionOperation> operations = keywords.stream()
                .filter(keyword -> keyword != null && !keyword.trim().isEmpty())
                .map(keyword -> CampaignCriterionOperation.newBuilder()
                        .setCreate(CampaignCriterion.newBuilder()
                                .setCampaign(ResourceNames.campaign(numericCustomerId, campaignId))
                                .setNegative(true)
                                .setKeyword(KeywordInfo.newBuilder()
                                        .setText(keyword.trim())
                                        .setMatchType(matchType)
                                        .build())
                                .build())
                        .build())
                .toList();
        try (CampaignCriterionServiceClient service = client.getLatestVersion().createCampaignCriterionServiceClient()) {
            MutateCampaignCriteriaResponse response = service.mutateCampaignCriteria(customerId, operations);
            return Map.of("decision", decision, "response", response.toString());
        }
    }

    private static int clampLimit(int limit) {
        if (limit <= 0) {
            return 100;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static Map<String, Object> rowToMap(GoogleAdsRow row) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (row.hasCampaign()) {
            map.put("campaignId", row.getCampaign().getId());
            map.put("campaignName", row.getCampaign().getName());
            map.put("campaignStatus", row.getCampaign().getStatus().name());
        }
        if (row.hasAdGroup()) {
            map.put("adGroupId", row.getAdGroup().getId());
            map.put("adGroupName", row.getAdGroup().getName());
        }
        if (row.hasCampaignBudget()) {
            map.put("campaignBudgetAmountMicros", row.getCampaignBudget().getAmountMicros());
            map.put("campaignBudgetResourceName", row.getCampaignBudget().getResourceName());
        }
        if (row.hasSearchTermView()) {
            map.put("searchTerm", row.getSearchTermView().getSearchTerm());
        }
        if (row.hasGeographicView()) {
            map.put("countryCriterionId", row.getGeographicView().getCountryCriterionId());
        }
        if (row.hasMetrics()) {
            map.put("impressions", row.getMetrics().getImpressions());
            map.put("clicks", row.getMetrics().getClicks());
            map.put("costMicros", row.getMetrics().getCostMicros());
            map.put("conversions", row.getMetrics().getConversions());
            map.put("conversionValue", row.getMetrics().getConversionsValue());
        }
        return map;
    }
}
