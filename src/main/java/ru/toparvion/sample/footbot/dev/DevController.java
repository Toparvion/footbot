package ru.toparvion.sample.footbot.dev;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.toparvion.sample.footbot.model.config.Schedule;

import java.io.IOException;
import java.time.ZonedDateTime;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

/**
 * @author Toparvion
 * @since v0.0.1
 */
@RestController
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class DevController {

  private final Schedule schedule;

  @GetMapping("/")
  public String index() {
    String now = ZonedDateTime.now().toString();
    log.info("Запрошен корневой адрес сервиса в момент: {}", now);
    return format("Привет от FootBot! (%s)", now);
  }

  @GetMapping("/schedule")
  public Schedule schedule() {
    return schedule;
  }

  @GetMapping(value = "/services/match/football/{matchId}/online/se/", produces = APPLICATION_JSON_UTF8_VALUE)
  public String events(@PathVariable String matchId) throws IOException {
    ClassPathResource mockResource = new ClassPathResource(format("mock/%s.json", matchId));
    String mockJson = FileUtils.readFileToString(mockResource.getFile(), UTF_8);
    String now = RFC_1123_DATE_TIME.format(ZonedDateTime.now());
    log.info("Подставляю в ответ текущее время: {}", now);
    return mockJson.replace("${now}", now);
  }

}
