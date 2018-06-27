package ru.toparvion.sample.footbot.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.toparvion.sample.footbot.flow.BroadcastFlowConfig;
import ru.toparvion.sample.footbot.model.config.Schedule;
import ru.toparvion.sample.footbot.model.sportexpress.event.Type;

import java.time.ZonedDateTime;

import static org.springframework.http.HttpStatus.I_AM_A_TEAPOT;

/**
 * @author Toparvion
 * @since v0.0.1
 */
@RestController
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class IndexController {

  private final Schedule schedule;
  private final BroadcastFlowConfig flowConfig;

  @GetMapping("/")
  public String index() {
    String now = ZonedDateTime.now().toString();
    log.info("Запрошен корневой адрес сервиса в момент: {}", now);
    return String.format("Привет от FootBot! (%s)", now);
  }

  @GetMapping("/schedule")
  public Schedule schedule() {
    return schedule;
  }

  @GetMapping("/register-user/{userId}/{level}")
  @ResponseStatus(I_AM_A_TEAPOT)
  public void registerUser(@PathVariable int userId, @PathVariable Type level) {
    flowConfig.startUserFlow(userId, level);
  }

}
