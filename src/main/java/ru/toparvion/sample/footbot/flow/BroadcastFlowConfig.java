package ru.toparvion.sample.footbot.flow;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.integration.dsl.Pollers.fixedDelay;

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
    return IntegrationFlows.from(matchEventsProvider,
                                 spec -> spec.poller(fixedDelay(15, SECONDS, 0)))
        .log()
        .get();
  }

}
