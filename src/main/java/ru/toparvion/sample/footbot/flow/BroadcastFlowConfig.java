package ru.toparvion.sample.footbot.flow;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import ru.toparvion.sample.footbot.model.sportexpress.event.Event;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.integration.dsl.IntegrationFlows.from;
import static org.springframework.integration.dsl.Pollers.fixedDelay;
import static org.springframework.util.StringUtils.hasText;
import static ru.toparvion.sample.footbot.model.sportexpress.event.Type.text;

/**
 * Основная конфигурация интеграционного конвейера
 */
@Configuration
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class BroadcastFlowConfig {

  /**
   * Общая часть конвейера (единственная)
   */
  @Bean
  public IntegrationFlow broadcastFlow(MatchEventsProvider matchEventsProvider) {
    return from(matchEventsProvider, spec -> spec.poller(fixedDelay(15, SECONDS, 0)))
          .split()
          .filter(Event.class, event -> !(event.getType() == text && !hasText(event.getText())))
          .<Event>log(message -> String.format("Получено событие матча с типом %s, временем %s и комментарием: %s",
              message.getPayload().getType(), message.getPayload().getFullMinute(), message.getPayload().getText()))
          .get();
  }

}
