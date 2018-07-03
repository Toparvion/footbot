package ru.toparvion.sample.footbot.flow;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowRegistration;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;
import org.springframework.integration.jdbc.metadata.JdbcMetadataStore;
import org.springframework.integration.selector.MetadataStoreSelector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import ru.toparvion.sample.footbot.model.sportexpress.event.Event;
import ru.toparvion.sample.footbot.model.sportexpress.event.Type;
import ru.toparvion.sample.footbot.util.Util;

import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.integration.dsl.IntegrationFlows.from;
import static org.springframework.integration.dsl.Pollers.fixedDelay;
import static org.springframework.integration.dsl.channel.MessageChannels.publishSubscribe;
import static org.springframework.integration.handler.LoggingHandler.Level.TRACE;
import static org.springframework.util.StringUtils.hasText;
import static ru.toparvion.sample.footbot.model.sportexpress.event.Type.*;
import static ru.toparvion.sample.footbot.util.IntegrationConstants.*;

/**
 * Основная конфигурация интеграционного конвейера
 */
@Configuration
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class BroadcastFlowConfig {

  private final IntegrationFlowContext flowContext;

  /**
   * Общая часть конвейера (единственная)
   */
  @Bean
  public IntegrationFlow broadcastFlow(MatchEventsProvider matchEventsProvider,
                                       IdempotentReceiverInterceptor antiDuplicateSubFilter) {
    return from(matchEventsProvider, spec -> spec.poller(fixedDelay(30, SECONDS, 20)))
          .split()
          .filter(Event.class, event -> !(event.getType() == text && !hasText(event.getText())),
                  spec -> spec.advice(antiDuplicateSubFilter))
          .channel(publishSubscribe(BROADCAST_CHANNEL))
          .get();
  }

  /**
   * Пользовательская часть конвейера (множественная)
   */
  public void startUserFlow(int userId, Type level, GenericHandler<Event> sendingCallback) {
    String userFlowId = USER_FLOW_PREFIX + userId;
    if (flowContext.getRegistrationById(userFlowId) != null) {
      flowContext.remove(userFlowId);
      log.debug("Предыдущая подписка с id={} найдена и удалена.", userFlowId);
    }
    // Задаем участок конвейера...
    StandardIntegrationFlow userFlow =
        from(BROADCAST_CHANNEL)
            .filter(Event.class, event -> filterRelevantEvent(event, level))
            .enrichHeaders(singletonMap(USER_ID_HEADER, userId))
            .log(BroadcastFlowConfig::composeEventLogRecord)
            .handle(Event.class, sendingCallback)
            .log(TRACE, this::saveMessageId)
            .get();
    // ... и тут же вводим его в эксплуатацию.
    IntegrationFlowRegistration userFlowRegistration =
        flowContext.registration(userFlow)
            .autoStartup(true)
            .id(userFlowId)
            .register();
    log.info("Зарегистрирован подписчик {} с уровнем {} (регистрация {})", userId, level,
        userFlowRegistration.getId());
  }

  private boolean filterRelevantEvent(Event event, Type userLevel) {
    Type eventLevel = event.getType();
    if (eventLevel.compareTo(none) > 0) {
      // every penalty event is considered equal to a goal
      eventLevel = goal;
    }
    return eventLevel.compareTo(userLevel) >= 0;
  }

  private static String composeEventLogRecord(Message<Event> message) {
    return String.format("Пользователь %d, событие матча: тип=%s, время=%s, комментарий=%s",
            message.getHeaders().get(USER_ID_HEADER, Integer.class), message.getPayload().getType(),
            message.getPayload().getFullMinute(), message.getPayload().getText());
  }

  private String saveMessageId(Message<Integer> message) {
    // TODO пока просто логируем
    return String.format("Сообщение с id=%s отправлено пользователю %s", message.getPayload(),
        message.getHeaders().get(USER_ID_HEADER));
  }

  /**
   * Фильтр для защиты от уже обработанных событий (дубликатов)
   */
  @Bean
  public IdempotentReceiverInterceptor antiDuplicateSubFilter(JdbcMetadataStore metadataStore) {
    // ключами хранилища считаем хэш всего событий
    // значениями - текст или временную метку (просто для удобства)
    MessageProcessor<String> valueStrategy = message -> Util.nvls(
        ((Event) message.getPayload()).getText(),
        requireNonNull(message.getHeaders().getTimestamp()).toString());
    // создаем селектор и перехватчикка
    MetadataStoreSelector messageSelector = new MetadataStoreSelector(this::composeUniqueEventId, valueStrategy, metadataStore);
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

  private String composeUniqueEventId(Message<?> eventMessage) {
    Event event = (Event) eventMessage.getPayload();
    String eventId = event.getId();
    String matchId = eventMessage.getHeaders().get(MATCH_ID_HEADER, String.class);
    return matchId + '_' + eventId;
  }

}
