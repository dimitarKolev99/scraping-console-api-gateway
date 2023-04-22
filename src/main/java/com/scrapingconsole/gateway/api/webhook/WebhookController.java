package com.scrapingconsole.gateway.api.webhook;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.scrapingconsole.gateway.api.webhook.dto.request.ScrapeHtmlRequest;
import com.scrapingconsole.gateway.api.webhook.dto.request.ScrapedData;
import com.scrapingconsole.gateway.api.webhook.dto.request.ScrapedHtml;
import com.scrapingconsole.gateway.api.webhook.dto.response.ScrapeHtmlResponse;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

@Controller
public class WebhookController {

    @Autowired
    private SimpMessagingTemplate template;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoOperations mongoOperations;

    @Autowired
    private WebClient scraperClient;


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

//        Document doc = Document.parse(json);
//        mongoTemplate.insert(doc, "scrapes");

//        MongoCollection<Document> robosCollection = mongoTemplate.getCollection("collectionName"); //get the name of the collection that you want

//        MongoCursor<Document> cursor =  robosCollection.find().cursor();//Mongo Cursor interface implementing the iterator protocol

//        cursor.forEachRemaining(System.out::println); //print all documents in Collection using method reference
//        mongoTemplate.findAll(Document.class, "scrapes");
//        LOGGER.debug("Retrieved data form db: {}", mongoTemplate.findAll(Document.class, "scrapes"));

        template.convertAndSend("/topic/scrapeddata", json);

        return new ResponseEntity<>(request, HttpStatus.OK);
    }

    @RequestMapping(value = "/scrapedhtml", method = RequestMethod.POST)
    public ResponseEntity<ScrapedHtml> scrapedHtml(@RequestBody ScrapedHtml request) {

        Gson gson = new Gson();

        String json = gson.toJson(request); // the url

        String url = request.getData()[1];
        // this needs to go into getHtml() mapping and not in this webhook method
        Query query = new BasicQuery(String.valueOf(new BasicDBObject("data", new BasicDBObject("$eq", url))));
        long count = mongoTemplate.count(query, "scrapedhtml");
        if (count > 0) {
            List<BasicDBObject> documents = mongoTemplate.getCollection("scrapedhtml").find(query.getQueryObject(), BasicDBObject.class).into(new ArrayList<>());
            BasicDBObject document = documents.get(0);
            List<String> dataArray = (List<String>) document.get("data");
            String firstElement = dataArray.get(0);

            LOGGER.debug("EXISTS: {}", firstElement);
        } else {
            LOGGER.debug("DOESNT exist");
            Document doc = Document.parse(json);
            mongoTemplate.insert(doc, "scrapedhtml");
        }

        template.convertAndSend("/topic/scrapeddata", json);

        return new ResponseEntity<>(request, HttpStatus.OK);
    }

    @PostMapping("/scrapehtml")
    @ResponseBody
    public ScrapeHtmlResponse getHtml(@RequestBody ScrapeHtmlRequest request) {

        String url = request.getUrl();
        String firstElement = null;

        // this needs to go into getHtml() mapping and not in this webhook method
        Query query = new BasicQuery(String.valueOf(new BasicDBObject("data.1", new BasicDBObject("$eq", url))));
        long count = mongoTemplate.count(query, "scrapedhtml");
        if (count > 0) {
            List<BasicDBObject> documents = mongoTemplate.getCollection("scrapedhtml").find(query.getQueryObject(), BasicDBObject.class).into(new ArrayList<>());
            BasicDBObject document = documents.get(0);
            List<String> dataArray = (List<String>) document.get("data");
            firstElement = dataArray.get(0);

            ScrapeHtmlResponse response = new ScrapeHtmlResponse();
            response.setResponse(firstElement);
            return response;

        } else {
            String callUrl = "/html";

            ResponseEntity<ScrapeHtmlResponse> responseEntity = scraperClient.post()
                    .uri(uriBuilder -> uriBuilder.path(callUrl).build())
                    .body(BodyInserters.fromValue(request))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .toEntity(ScrapeHtmlResponse.class)
                    .doOnError(e -> LOGGER.error("Error retrieving response from scraper service: {}", e.getMessage()))
                    .onErrorReturn(ResponseEntity.ok(new ScrapeHtmlResponse()))
                    .block();

            return responseEntity != null && responseEntity.hasBody() ? responseEntity.getBody() : null;
        }

    }


}
