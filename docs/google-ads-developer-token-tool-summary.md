---
title: "Google Ads API Developer Token Application Support Document"
subtitle: "Google Marketing MCP - Tool Description and Controls"
author: "[Applicant / Company Name]"
date: "2026-05-21"
geometry: margin=0.75in
fontsize: 10pt
---

# Purpose

This document describes the Google Marketing MCP tool that will use the Google Ads API. It is intended to support a Google Ads API developer token application by explaining the tool's use case, API scope, controls, and data handling.

# Applicant Details

| Field | Value |
| --- | --- |
| Applicant / company name | [Applicant / Company Name] |
| Primary contact email | [API Contact Email] |
| Google Ads manager account ID | [Manager Account ID] |
| Google Ads customer account IDs | [Customer Account ID(s)] |
| Website / product URL | [Website or Repository URL] |
| Intended users | Internal marketing and engineering users only |

# Tool Overview

Google Marketing MCP is a standalone Spring Boot Model Context Protocol server for Google Ads and Google Analytics 4 reporting and guarded campaign operations. The tool runs locally or in controlled infrastructure and exposes a small set of MCP tools for campaign optimization workflows.

The tool is designed for internal campaign analysis and controlled optimization of accounts owned or managed by the applicant. It is not a public advertising platform, agency marketplace, lead resale product, or third-party self-service application.

# Intended Google Ads API Use

The requested developer token is needed so the tool can read Google Ads campaign performance and, after explicit approval, apply limited optimization actions.

Primary use cases:

- Read campaign-level performance, including impressions, clicks, cost, conversions, conversion value, campaign status, and budget amount.
- Read country-level performance for geographic optimization.
- Read search term performance to identify wasteful or irrelevant queries.
- Run read-only GAQL SELECT reports for account diagnostics and optimization analysis.
- Add campaign-level negative keywords when search terms show irrelevant spend.
- Pause or enable campaigns when explicitly approved by an authorized operator.
- Update campaign budgets within configured percentage guardrails when explicitly approved by an authorized operator.

# Requested Access

The tool requires production Google Ads account access for reporting and guarded campaign management.

Recommended requested access level:

- Basic Access, if the expected usage is within 15,000 operations per day.
- Standard Access only if account scale or automation volume requires higher daily operation limits.

Recommended permissible use:

- Reporting, for read-only analysis.
- Ad creation / management, because the tool can add negative keywords, pause or enable campaigns, and update campaign budgets after authorization.

# Google Ads API Services and Methods

The implementation uses the official Google Ads Java client library and Google Ads API resources exposed through the current client version.

Expected API activity:

| Capability | Google Ads API usage |
| --- | --- |
| Campaign performance reporting | `GoogleAdsService.Search` with GAQL SELECT queries |
| Country performance reporting | `GoogleAdsService.Search` with GAQL SELECT queries |
| Search term reporting | `GoogleAdsService.Search` with GAQL SELECT queries |
| Custom diagnostics | `GoogleAdsService.Search` with read-only GAQL SELECT queries |
| Campaign status changes | `CampaignService.MutateCampaigns` |
| Budget updates | `CampaignBudgetService.MutateCampaignBudgets` |
| Negative keyword additions | `CampaignCriterionService.MutateCampaignCriteria` |

The custom GAQL tool blocks non-SELECT queries before execution.

# Current Tool Surface

Read-only MCP tools:

- `ads_campaign_performance`
- `ads_country_performance`
- `ads_search_terms`
- `ads_run_gaql`
- `ga4_traffic_acquisition`
- `ga4_landing_pages`
- `ga4_events`
- `ga4_run_report`

Guarded Google Ads write tools:

- `ads_pause_campaign`
- `ads_enable_campaign`
- `ads_update_campaign_budget`
- `ads_add_campaign_negative_keywords`

# Optimization Workflow

1. The operator runs read-only reports for a selected date range.
2. The tool analyzes campaign performance, search terms, countries, landing pages, and conversion outcomes.
3. The tool produces recommended changes with campaign IDs, budget IDs, current values, proposed values, and rationale.
4. An authorized operator reviews the proposed changes.
5. Write tools remain blocked unless write mode is enabled.
6. Write tools remain dry-run by default unless dry-run mode is explicitly disabled.
7. A confirmation token is required by default for write actions.
8. The tool logs each attempted write decision and payload to a local JSONL change log.

# Safety Controls

The tool has multiple controls intended to prevent accidental or unauthorized spend changes:

- Write mode is disabled by default through `MARKETING_MCP_WRITE_MODE=false`.
- Dry-run mode is enabled by default through `MARKETING_MCP_DRY_RUN=true`.
- Confirmation tokens are required by default through `MARKETING_MCP_REQUIRE_CONFIRMATION_TOKEN=true`.
- Campaign budget changes are limited by `MARKETING_MCP_MAX_DAILY_BUDGET_CHANGE_PERCENT`, defaulting to 20%.
- The custom GAQL endpoint only accepts queries that begin with SELECT.
- Write actions are limited to campaign status, campaign budget amount, and campaign-level negative keywords.
- The tool does not create accounts, change billing, invite users, create broad campaign structures, or modify payment settings.
- Change attempts are logged to `MARKETING_MCP_CHANGE_LOG`, defaulting to `./logs/marketing-mcp-changes.jsonl`.

# Data Access and Privacy

The tool accesses Google Ads campaign performance data and GA4 reporting data for accounts configured by the applicant. Data may include campaign names, campaign IDs, ad group names, search terms, country identifiers, device categories, landing page paths, event names, costs, clicks, impressions, conversions, and revenue metrics.

The tool does not require end-user personal information. It is intended to process aggregated advertising and analytics performance data for optimization and reporting.

Credentials are supplied through environment variables or Google Application Credentials and are not hard-coded in the source code.

Required Google Ads configuration:

- `GOOGLE_ADS_DEVELOPER_TOKEN`
- `GOOGLE_ADS_CLIENT_ID`
- `GOOGLE_ADS_CLIENT_SECRET`
- `GOOGLE_ADS_REFRESH_TOKEN`
- `GOOGLE_ADS_CUSTOMER_ID`
- `GOOGLE_ADS_LOGIN_CUSTOMER_ID`, only when using a manager account

Optional GA4 configuration:

- `GA4_PROPERTY_ID`
- `GOOGLE_APPLICATION_CREDENTIALS`

# Authentication Model

The Google Ads API integration uses OAuth client credentials and a refresh token for authorized account access. The GA4 integration uses Google Application Credentials for service account authentication where configured.

Only authorized operators with access to the configured runtime environment and credentials can use the tool against production accounts.

# Non-Goals and Explicit Exclusions

The tool is not designed to:

- Create Google Ads accounts.
- Manage billing or payments.
- Invite or manage Google Ads users.
- Scrape Google Ads UI pages.
- Circumvent Google Ads policies or quota controls.
- Serve ads on behalf of unknown third parties.
- Sell or expose Google Ads account data to external users.

# Operational Limits

The tool caps individual report responses at 500 rows. Read operations are intended for periodic optimization analysis, not high-volume data extraction. Write operations require explicit runtime configuration and approval controls.

# References

Google Ads API documentation states that a developer token is required to make Google Ads API calls and that the token's access level controls daily API usage and whether production accounts can be accessed.

Google's access level documentation describes Basic Access as supporting test and production accounts with a 15,000 operation-per-day limit, and Standard Access as supporting test and production accounts with unlimited operations for most services. It also describes permissible use categories, including Reporting and Ad creation / management.

Sources:

- Google Ads API Developer Token: https://developers.google.com/google-ads/api/docs/get-started/dev-token
- Google Ads API Access Levels and Permissible Use: https://developers.google.com/google-ads/api/docs/api-policy/access-levels

