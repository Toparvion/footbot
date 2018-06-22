package ru.toparvion.sample.footbot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;

/**
 * @author Toparvion
 * @since v0.0.1
 */
@RestController
@Slf4j
public class IndexController {

  @GetMapping("/")
  public String index() {
    String now = ZonedDateTime.now().toString();
    log.info("Запрошен корневой адрес сервиса в момент: {}", now);
    return String.format("Привет от FootBot! (%s)", now);
  }

}
