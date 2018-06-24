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
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import ru.toparvion.sample.footbot.model.sportexpress.event.Event;
import ru.toparvion.sample.footbot.telegram.FootBot;
import ru.toparvion.sample.footbot.util.Util;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.integration.dsl.IntegrationFlows.from;
import static org.springframework.integration.dsl.Pollers.fixedDelay;

@Configuration
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class BroadcastFlowConfig {

  private final FootBot footBot;
  private final MatchEventsProvider matchEventsProvider;

  @Bean
  public IntegrationFlow broadcastFlow(IdempotentReceiverInterceptor antiDuplicateSubFilter) {
    return from(matchEventsProvider, spec -> spec.poller(fixedDelay(60, SECONDS, 0)))
            .split()
            .filter(Event.class,
                    event -> true,
                    //event -> (event.getType() != Type.text),
                    conf -> conf.advice(antiDuplicateSubFilter))
            .transform(Event.class, this::composeEventText)
            .handle(message -> sendEventViaBot((String) message.getPayload()))
            .get();
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

  private void sendEventViaBot(String eventText) {
    try {
      Message sentMessage = footBot.execute(new SendMessage((long) footBot.creatorId(), eventText));
      log.info("Отправлено Telegram сообщение с id={}: {}", sentMessage.getMessageId(), sentMessage.getText());

    } catch (TelegramApiException e) {
      log.error("Не удалось отправить сообщение.", e);
    }
  }

  private String composeEventText(Event event) {
    switch (event.getType()) {
      case text:
        return event.getText();
      case change:
        return String.format("Замена (%s): %s -> %s", event.getCommand().getName(),
                event.getPlayerOut().getName(), event.getPlayerIn().getName());
      case statechange:
        return event.getChangedStateName();
      case goal:
        return String.format("%s\n(Автор гола: %s (%s), текущий счёт: %s)", event.getText(),
                event.getPlayer().getName(), event.getCommand().getName(), event.getInfo().getScore());
      case card:
        StringBuilder sb = new StringBuilder();
        switch (event.getKind()) {
          case yellow:
            sb.append("Жёлтая");
            break;
          case yellow2:
            sb.append("Вторая жёлтая");
            break;
          case red:
            sb.append("Красная");
            break;
        }
        sb.append(" карточка: ")
                .append(event.getPlayer().getName())
                .append(" (")
                .append(event.getCommand().getName())
                .append(")");
        return sb.toString();
      default:
        return event.getText();
    }
  }

}
