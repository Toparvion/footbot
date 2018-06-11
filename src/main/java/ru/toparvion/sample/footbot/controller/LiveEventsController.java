package ru.toparvion.sample.footbot.controller;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.toparvion.sample.footbot.flow.LiveEventsGateway;
import ru.toparvion.sample.footbot.model.livescore.Event;
import ru.toparvion.sample.footbot.model.livescore.LivescoreResponse;

/**
 * @author Toparvion
 * @since v0.0.1
 */
@RestController
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class LiveEventsController {

  private final LiveEventsGateway gateway;

  @GetMapping("/events/{matchId}")
  public LivescoreResponse<Event> getEvents(@PathVariable String matchId) {
    return gateway.fetchEvents(matchId);
  }
}
