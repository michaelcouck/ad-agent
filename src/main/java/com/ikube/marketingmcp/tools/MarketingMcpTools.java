package com.ikube.marketingmcp.tools;

import java.util.List;

import com.google.ads.googleads.v23.enums.CampaignStatusEnum.CampaignStatus;
import com.google.ads.googleads.v23.enums.KeywordMatchTypeEnum.KeywordMatchType;
import com.ikube.marketingmcp.ga4.Ga4ReportingService;
import com.ikube.marketingmcp.googleads.GoogleAdsReportingService;
import com.ikube.marketingmcp.model.ToolResult;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class MarketingMcpTools {

    private final GoogleAdsReportingService googleAds;
    private final Ga4ReportingService ga4;

    public MarketingMcpTools(GoogleAdsReportingService googleAds, Ga4ReportingService ga4) {
        this.googleAds = googleAds;
        this.ga4 = ga4;
    }

    @Tool("Read Google Ads campaign performance for a date range.")
    @McpTool(name = "ads_campaign_performance", description = "Read Google Ads campaign performance for a date range.")
    public ToolResult adsCampaignPerformance(
            @P("Start date in YYYY-MM-DD format") @McpToolParam(description = "Start date in YYYY-MM-DD format") String since,
            @P("End date in YYYY-MM-DD format") @McpToolParam(description = "End date in YYYY-MM-DD format") String until,
            @P("Maximum number of rows, capped at 500") @McpToolParam(description = "Maximum number of rows, capped at 500") int limit) {
        return ToolResult.ok(googleAds.campaignPerformance(since, until, limit));
    }

    @Tool("Read Google Ads performance by country for a date range.")
    @McpTool(name = "ads_country_performance", description = "Read Google Ads performance by country for a date range.")
    public ToolResult adsCountryPerformance(
            @P("Start date in YYYY-MM-DD format") @McpToolParam(description = "Start date in YYYY-MM-DD format") String since,
            @P("End date in YYYY-MM-DD format") @McpToolParam(description = "End date in YYYY-MM-DD format") String until,
            @P("Maximum number of rows, capped at 500") @McpToolParam(description = "Maximum number of rows, capped at 500") int limit) {
        return ToolResult.ok(googleAds.countryPerformance(since, until, limit));
    }

    @Tool("Read Google Ads search terms for a date range.")
    @McpTool(name = "ads_search_terms", description = "Read Google Ads search terms for a date range.")
    public ToolResult adsSearchTerms(
            @P("Start date in YYYY-MM-DD format") @McpToolParam(description = "Start date in YYYY-MM-DD format") String since,
            @P("End date in YYYY-MM-DD format") @McpToolParam(description = "End date in YYYY-MM-DD format") String until,
            @P("Maximum number of rows, capped at 500") @McpToolParam(description = "Maximum number of rows, capped at 500") int limit) {
        return ToolResult.ok(googleAds.searchTerms(since, until, limit));
    }

    @Tool("Run a read-only Google Ads Query Language query. Use only SELECT queries.")
    @McpTool(name = "ads_run_gaql", description = "Run a read-only Google Ads Query Language query. Use only SELECT queries.")
    public ToolResult adsRunGaql(
            @P("GAQL SELECT query") @McpToolParam(description = "GAQL SELECT query") String query,
            @P("Maximum number of rows, capped at 500") @McpToolParam(description = "Maximum number of rows, capped at 500") int limit) {
        if (!query.trim().toLowerCase().startsWith("select")) {
            return ToolResult.blocked("Only SELECT GAQL queries are allowed.", null);
        }
        return ToolResult.ok(googleAds.runGaql(query, limit));
    }

    @Tool("Pause a Google Ads campaign. Write mode and confirmation token are required unless dry-run is enabled.")
    @McpTool(name = "ads_pause_campaign", description = "Pause a Google Ads campaign. Guarded write tool.")
    public ToolResult adsPauseCampaign(
            @P("Google Ads campaign ID") @McpToolParam(description = "Google Ads campaign ID") long campaignId,
            @P("Confirmation token") @McpToolParam(description = "Confirmation token", required = false) String confirmationToken) {
        return ToolResult.ok(googleAds.setCampaignStatus(campaignId, CampaignStatus.PAUSED, confirmationToken));
    }

    @Tool("Enable a Google Ads campaign. Write mode and confirmation token are required unless dry-run is enabled.")
    @McpTool(name = "ads_enable_campaign", description = "Enable a Google Ads campaign. Guarded write tool.")
    public ToolResult adsEnableCampaign(
            @P("Google Ads campaign ID") @McpToolParam(description = "Google Ads campaign ID") long campaignId,
            @P("Confirmation token") @McpToolParam(description = "Confirmation token", required = false) String confirmationToken) {
        return ToolResult.ok(googleAds.setCampaignStatus(campaignId, CampaignStatus.ENABLED, confirmationToken));
    }

    @Tool("Update a Google Ads campaign budget in micros. Guarded by max percentage change.")
    @McpTool(name = "ads_update_campaign_budget", description = "Update a Google Ads campaign budget in micros. Guarded write tool.")
    public ToolResult adsUpdateCampaignBudget(
            @P("Google Ads campaign budget ID") @McpToolParam(description = "Google Ads campaign budget ID") long campaignBudgetId,
            @P("Current budget amount in micros") @McpToolParam(description = "Current budget amount in micros") long currentBudgetMicros,
            @P("Proposed budget amount in micros") @McpToolParam(description = "Proposed budget amount in micros") long proposedBudgetMicros,
            @P("Confirmation token") @McpToolParam(description = "Confirmation token", required = false) String confirmationToken) {
        return ToolResult.ok(googleAds.updateCampaignBudgetMicros(
                campaignBudgetId,
                currentBudgetMicros,
                proposedBudgetMicros,
                confirmationToken));
    }

    @Tool("Add campaign-level negative keywords. Guarded write tool.")
    @McpTool(name = "ads_add_campaign_negative_keywords", description = "Add campaign-level negative keywords. Guarded write tool.")
    public ToolResult adsAddCampaignNegativeKeywords(
            @P("Google Ads campaign ID") @McpToolParam(description = "Google Ads campaign ID") long campaignId,
            @P("Negative keywords to add") @McpToolParam(description = "Negative keywords to add") List<String> keywords,
            @P("Match type: EXACT, PHRASE, or BROAD") @McpToolParam(description = "Match type: EXACT, PHRASE, or BROAD") String matchType,
            @P("Confirmation token") @McpToolParam(description = "Confirmation token", required = false) String confirmationToken) {
        return ToolResult.ok(googleAds.addCampaignNegativeKeywords(
                campaignId,
                keywords,
                KeywordMatchType.valueOf(matchType.trim().toUpperCase()),
                confirmationToken));
    }

    @Tool("Read GA4 traffic acquisition by source, campaign, country, and device.")
    @McpTool(name = "ga4_traffic_acquisition", description = "Read GA4 traffic acquisition by source, campaign, country, and device.")
    public ToolResult ga4TrafficAcquisition(
            @P("Start date in YYYY-MM-DD format") @McpToolParam(description = "Start date in YYYY-MM-DD format") String since,
            @P("End date in YYYY-MM-DD format") @McpToolParam(description = "End date in YYYY-MM-DD format") String until,
            @P("Maximum number of rows, capped at 500") @McpToolParam(description = "Maximum number of rows, capped at 500") int limit) {
        return ToolResult.ok(ga4.trafficAcquisition(since, until, limit));
    }

    @Tool("Read GA4 landing page performance by campaign, source, and country.")
    @McpTool(name = "ga4_landing_pages", description = "Read GA4 landing page performance by campaign, source, and country.")
    public ToolResult ga4LandingPages(
            @P("Start date in YYYY-MM-DD format") @McpToolParam(description = "Start date in YYYY-MM-DD format") String since,
            @P("End date in YYYY-MM-DD format") @McpToolParam(description = "End date in YYYY-MM-DD format") String until,
            @P("Maximum number of rows, capped at 500") @McpToolParam(description = "Maximum number of rows, capped at 500") int limit) {
        return ToolResult.ok(ga4.landingPages(since, until, limit));
    }

    @Tool("Read GA4 event performance by campaign, source, and country.")
    @McpTool(name = "ga4_events", description = "Read GA4 event performance by campaign, source, and country.")
    public ToolResult ga4Events(
            @P("Start date in YYYY-MM-DD format") @McpToolParam(description = "Start date in YYYY-MM-DD format") String since,
            @P("End date in YYYY-MM-DD format") @McpToolParam(description = "End date in YYYY-MM-DD format") String until,
            @P("Maximum number of rows, capped at 500") @McpToolParam(description = "Maximum number of rows, capped at 500") int limit) {
        return ToolResult.ok(ga4.events(since, until, limit));
    }

    @Tool("Run a GA4 report with custom dimensions and metrics.")
    @McpTool(name = "ga4_run_report", description = "Run a GA4 report with custom dimensions and metrics.")
    public ToolResult ga4RunReport(
            @P("Start date in YYYY-MM-DD format") @McpToolParam(description = "Start date in YYYY-MM-DD format") String since,
            @P("End date in YYYY-MM-DD format") @McpToolParam(description = "End date in YYYY-MM-DD format") String until,
            @P("GA4 dimensions") @McpToolParam(description = "GA4 dimensions") List<String> dimensions,
            @P("GA4 metrics") @McpToolParam(description = "GA4 metrics") List<String> metrics,
            @P("Maximum number of rows, capped at 500") @McpToolParam(description = "Maximum number of rows, capped at 500") int limit) {
        return ToolResult.ok(ga4.runReport(since, until, dimensions, metrics, limit));
    }
}
