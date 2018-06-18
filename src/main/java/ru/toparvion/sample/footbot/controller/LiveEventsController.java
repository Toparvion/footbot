package ru.toparvion.sample.footbot.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import ru.toparvion.sample.footbot.flow.LiveEventsGateway;
import ru.toparvion.sample.footbot.model.livescore.Event;
import ru.toparvion.sample.footbot.model.livescore.LivescoreResponse;

/**
 * @author Toparvion
 * @since v0.0.1
 */
@RestController
@AllArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class LiveEventsController {

  private final LiveEventsGateway gateway;

  @GetMapping("/events/{matchId}")
  public LivescoreResponse<Event> getEvents(@PathVariable String matchId) {
    return gateway.fetchEvents(matchId);
  }

  @GetMapping("/dev/test-livescore-api")
  public Object testLivescoreApi() {
    log.info("Sending test request to Livescore API.");
    RestTemplate restTemplate = new RestTemplate();
    String uri = "http://livescore-api.com/api-client/users/pair.json?key=xj0WfwwQ48WqgMPD&secret=nQK8I0mav0MkKvrhOre4YKV3TRtaDjN0";
    return restTemplate.getForObject(uri, Object.class);
  }
}
