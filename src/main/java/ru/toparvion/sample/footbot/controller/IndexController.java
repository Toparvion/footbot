package ru.toparvion.sample.footbot.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.toparvion.sample.footbot.model.config.Schedule;

import java.time.ZonedDateTime;

/**
 * @author Toparvion
 * @since v0.0.1
 */
@RestController
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class IndexController {

  private final Schedule schedule;

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

}
