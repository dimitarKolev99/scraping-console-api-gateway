package com.scrapingconsole.gateway.api.websocket;

import com.scrapingconsole.gateway.api.webhook.dto.request.ScrapedData;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class DataController {

//    @MessageMapping("/datanotification")
//    @SendTo("/datanotification")
//    public ScrapedData returnScrapedData() {
//    }

}
