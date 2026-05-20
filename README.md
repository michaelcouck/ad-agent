# Google Marketing MCP

Standalone Spring Boot MCP server for Google Ads and GA4. It is intentionally separate from the legacy iKube Maven reactor because the existing project targets Java 8, while Spring AI MCP and the current MCP Java stack require Java 17+.

## What It Exposes

Read-only tools:

- `ads_campaign_performance`
- `ads_country_performance`
- `ads_search_terms`
- `ads_run_gaql`
- `ga4_traffic_acquisition`
- `ga4_landing_pages`
- `ga4_events`
- `ga4_run_report`

Guarded write tools:

- `ads_pause_campaign`
- `ads_enable_campaign`
- `ads_update_campaign_budget`
- `ads_add_campaign_negative_keywords`

Write tools are blocked by default. Set `MARKETING_MCP_WRITE_MODE=true` to permit writes. They still dry-run unless `MARKETING_MCP_DRY_RUN=false`. A confirmation token is required by default.

## Environment

Copy `.env.example` and fill in the values, or export the variables directly.

Required for Google Ads:

```bash
export GOOGLE_ADS_DEVELOPER_TOKEN="..."
export GOOGLE_ADS_CLIENT_ID="..."
export GOOGLE_ADS_CLIENT_SECRET="..."
export GOOGLE_ADS_REFRESH_TOKEN="..."
export GOOGLE_ADS_CUSTOMER_ID="1234567890"
export GOOGLE_ADS_LOGIN_CUSTOMER_ID="0987654321" # only if using an MCC
```

Required for GA4:

```bash
export GA4_PROPERTY_ID="123456789"
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account.json"
```

Optional write guards:

```bash
export MARKETING_MCP_WRITE_MODE=false
export MARKETING_MCP_DRY_RUN=true
export MARKETING_MCP_REQUIRE_CONFIRMATION_TOKEN=true
export MARKETING_MCP_CONFIRMATION_TOKEN="choose-a-long-random-value"
export MARKETING_MCP_MAX_DAILY_BUDGET_CHANGE_PERCENT=20
export MARKETING_MCP_CHANGE_LOG="./logs/marketing-mcp-changes.jsonl"
```

## Build

```bash
cd connectors/google-marketing-mcp
java -version # must be 17+
mvn -q -DskipTests package
```

## Run

The server uses stdio transport for local MCP clients:

```bash
cd connectors/google-marketing-mcp
./run.sh
```

Example MCP client command:

```json
{
  "mcpServers": {
    "google-marketing": {
      "command": "java",
      "args": [
        "-jar",
        "/home/laptop/Workspace/ikube/connectors/google-marketing-mcp/target/google-marketing-mcp-0.1.0-SNAPSHOT.jar"
      ],
      "env": {
        "GOOGLE_ADS_DEVELOPER_TOKEN": "...",
        "GOOGLE_ADS_CLIENT_ID": "...",
        "GOOGLE_ADS_CLIENT_SECRET": "...",
        "GOOGLE_ADS_REFRESH_TOKEN": "...",
        "GOOGLE_ADS_CUSTOMER_ID": "...",
        "GA4_PROPERTY_ID": "...",
        "GOOGLE_APPLICATION_CREDENTIALS": "/path/to/service-account.json"
      }
    }
  }
}
```

## First Test

Start read-only:

```bash
export MARKETING_MCP_WRITE_MODE=false
export MARKETING_MCP_DRY_RUN=true
```

Call:

- `ads_campaign_performance` for the last 7 days
- `ads_search_terms` for the last 7 days
- `ga4_traffic_acquisition` for the last 7 days

Only after those work should write mode be enabled, and initially with dry-run still on.
