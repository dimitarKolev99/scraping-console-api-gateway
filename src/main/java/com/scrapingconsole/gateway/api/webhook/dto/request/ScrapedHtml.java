package com.scrapingconsole.gateway.api.webhook.dto.request;

import lombok.Data;

public @Data class ScrapedHtml {
    private String[] data;
}
