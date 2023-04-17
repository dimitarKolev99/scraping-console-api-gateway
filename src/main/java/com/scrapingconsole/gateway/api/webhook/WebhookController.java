package com.scrapingconsole.gateway.api.webhook;

import com.google.gson.Gson;
import com.scrapingconsole.gateway.api.webhook.dto.request.ScrapedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class WebhookController {

    @Autowired
    private SimpMessagingTemplate template;

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookController.class);

    @MessageMapping("/hello")
    @SendTo("/topic/scrapeddata")
    public String greeting(String message) throws Exception {

        return message;
    }

    @RequestMapping(value = "/scraped", method = RequestMethod.POST)
    public ResponseEntity<ScrapedData> scrapedData(@RequestBody ScrapedData request) {

        LOGGER.debug("Request Body: {}", request);

        Gson gson = new Gson();

        String json = gson.toJson(request);

        template.convertAndSend("/topic/scrapeddata", json);

        return new ResponseEntity<>(request, HttpStatus.OK);
    }

}
