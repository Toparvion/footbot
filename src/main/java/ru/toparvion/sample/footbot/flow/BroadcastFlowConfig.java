package ru.toparvion.sample.footbot.flow;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;
import org.springframework.integration.jdbc.metadata.JdbcMetadataStore;
import org.springframework.integration.selector.MetadataStoreSelector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import ru.toparvion.sample.footbot.model.sportexpress.event.Event;
import ru.toparvion.sample.footbot.util.Util;

import static java.util.Objects.requireNonNull;
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
  public IntegrationFlow broadcastFlow(MatchEventsProvider matchEventsProvider,
                                       IdempotentReceiverInterceptor antiDuplicateSubFilter) {
    return from(matchEventsProvider, spec -> spec.poller(fixedDelay(15, SECONDS, 0)))
          .split()
          .filter(Event.class, event -> !(event.getType() == text && !hasText(event.getText())),
              conf -> conf.advice(antiDuplicateSubFilter))
          .log(BroadcastFlowConfig::composeEventLogRecord)
          .get();
  }

  private static String composeEventLogRecord(Message<Event> message) {
    return String.format("Событие матча: тип=%s, время=%s, комментарий=%s",
        message.getPayload().getType(), message.getPayload().getFullMinute(), message.getPayload().getText());
  }

  /**
   * Фильтр для защиты от уже обработанных событий (дубликатов)
   */
  @Bean
  public IdempotentReceiverInterceptor antiDuplicateSubFilter(JdbcMetadataStore metadataStore) {
    // ключами хранилища считаем хэш всего событий
    MessageProcessor<String> keyStrategy = message -> Integer.toHexString(message.getPayload().hashCode());
    // значениями - текст или временную метку (просто для удобства)
    MessageProcessor<String> valueStrategy = message -> Util.nvls(
        ((Event) message.getPayload()).getText(),
        requireNonNull(message.getHeaders().getTimestamp()).toString());
    // создаем селектор и перехватчикка
    MetadataStoreSelector messageSelector = new MetadataStoreSelector(keyStrategy, valueStrategy, metadataStore);
    IdempotentReceiverInterceptor interceptor = new IdempotentReceiverInterceptor(messageSelector);
    // дубликаты не задумываясь отправляем на помойку
    interceptor.setDiscardChannel(new NullChannel());
    interceptor.setThrowExceptionOnRejection(false);
    return interceptor;
  }

  /**
   * Персистентное хранилище метаданных о событиях
   */
  @Bean
  public JdbcMetadataStore metadataStore(JdbcTemplate jdbcTemplate) {
    return new JdbcMetadataStore(jdbcTemplate);
  }

}
