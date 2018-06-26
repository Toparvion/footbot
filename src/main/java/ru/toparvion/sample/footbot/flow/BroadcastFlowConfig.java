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
import static ru.toparvion.sample.footbot.util.IntegrationConstants.*;

@Configuration
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class BroadcastFlowConfig {

  private final MatchEventsProvider matchEventsProvider;
  private final IntegrationFlowContext flowContext;

  @Bean
  public IntegrationFlow broadcastFlow(IdempotentReceiverInterceptor antiDuplicateSubFilter) {
    return from(matchEventsProvider, spec -> spec.poller(fixedDelay(60, SECONDS, 60)))
        .split()
        .filter(Event.class,
            event -> true /*TODO можно вставить что-нибудь по-сложнее*/,
            conf -> conf.advice(antiDuplicateSubFilter))
        .channel(publishSubscribe(BROADCAST_CHANNEL))
        .get();
  }

  public void startUserFlow(int userId, Type level, GenericHandler<Event> sendingCallback) {
    String userFlowId = USER_FLOW_PREFIX + userId;
    if (flowContext.getRegistrationById(userFlowId) != null) {
      flowContext.remove(userFlowId);
      log.debug("Предыдущая подписка с id={} найдена и удалена.", userFlowId);
    }
    StandardIntegrationFlow userFlow =
        from(BROADCAST_CHANNEL)
            .filter(Event.class, event -> event.getType().compareTo(level) >= 0)
            .enrichHeaders(singletonMap(USER_ID_HEADER, userId))
            .handle(Event.class, sendingCallback)
            .log(this::saveMessageId)
            .get();
    IntegrationFlowRegistration userFlowRegistration =
        flowContext.registration(userFlow)
            .autoStartup(true)
            .id(userFlowId)
            .register();
    log.info("Зарегистрирован новый подписчик {} с уровнем {} (регистрация {})", userId, level,
        userFlowRegistration.getId());
  }

  private String saveMessageId(Message<Integer> message) {
    // TODO пока просто логируем
    return String.format("Сообщение с id=%s отправлено пользователю %s", message.getPayload(),
        message.getHeaders().get(USER_ID_HEADER));
  }

  @Bean
  public IdempotentReceiverInterceptor antiDuplicateSubFilter(JdbcMetadataStore metadataStore) {
    MessageProcessor<String> keyStrategy = message -> Integer.toHexString(message.getPayload().hashCode());
    MessageProcessor<String> valueStrategy = message -> Util.nvls(
        ((Event) message.getPayload()).getText(),
        requireNonNull(message.getHeaders().getTimestamp()).toString());
    MetadataStoreSelector messageSelector = new MetadataStoreSelector(keyStrategy, valueStrategy, metadataStore);
    IdempotentReceiverInterceptor interceptor = new IdempotentReceiverInterceptor(messageSelector);
    interceptor.setDiscardChannel(new NullChannel());
    interceptor.setThrowExceptionOnRejection(false);
    return interceptor;
  }

  @Bean
  public JdbcMetadataStore metadataStore(JdbcTemplate jdbcTemplate) {
    return new JdbcMetadataStore(jdbcTemplate);
  }

}
