package com.scrapingconsole.gateway.api.websocket;

import lombok.Data;

public @Data class WsDataResponse {
    private Object[] data;

    public WsDataResponse() {

    }
}
