package ru.toparvion.sample.footbot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.toparvion.sample.footbot.flow.LiveEventsGateway;
import ru.toparvion.sample.footbot.model.livescore.Event;
import ru.toparvion.sample.footbot.model.livescore.LivescoreResponse;

import java.net.URI;

/**
 * @author Toparvion
 * @since v0.0.1
 */
@RestController
@Slf4j
public class LiveEventsController {

  private final LiveEventsGateway gateway;
  private final String apiEndpoint;
  private final String apiKey;
  private final String apiSecret;

  @Autowired
  public LiveEventsController(LiveEventsGateway gateway,
                              @Value("${livescore.api.endpoint}") String apiEndpoint,
                              @Value("${livescore.api.key}") String apiKey,
                              @Value("${livescore.api.secret}") String apiSecret) {
    this.gateway = gateway;
    this.apiEndpoint = apiEndpoint;
    this.apiKey = apiKey;
    this.apiSecret = apiSecret;
  }

  @GetMapping("/")
  public String index() {
    return "Just Hello, Heroku!";
  }

  @GetMapping("/events/{matchId}")
  public LivescoreResponse<Event> getEvents(@PathVariable String matchId) {
    return gateway.fetchEvents(matchId);
  }

  @GetMapping("/dev/test-livescore-api")
  public Object testLivescoreApi() {
    log.info("Sending test request to Livescore API.");
    RestTemplate restTemplate = new RestTemplate();
    return restTemplate.getForObject(buildLivescoreUri(), Object.class);
  }

  private URI buildLivescoreUri() {
    URI uri = UriComponentsBuilder.fromUriString(apiEndpoint)
            .path("/users/pair.json")
            .queryParam("key", apiKey)
            .queryParam("secret", apiSecret)
            .build()
            .toUri();
    log.info("Livescore API request URI: {}", uri);
    return uri;
  }

}
