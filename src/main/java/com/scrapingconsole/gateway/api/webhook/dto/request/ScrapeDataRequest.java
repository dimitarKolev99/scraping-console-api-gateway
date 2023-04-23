package com.scrapingconsole.gateway.api.webhook.dto.request;

import lombok.Data;

public @Data class ScrapeDataRequest {
    private String listSelector;
    private String url;
    private Object[] parsingLogic;
    private String nextButton;
    private String maxPages;
}
