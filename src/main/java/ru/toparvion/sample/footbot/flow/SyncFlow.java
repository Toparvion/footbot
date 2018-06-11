package ru.toparvion.sample.footbot.flow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.dsl.Http;
import org.springframework.messaging.Message;
import org.springframework.web.util.UriComponentsBuilder;
import ru.toparvion.sample.footbot.model.livescore.Event;
import ru.toparvion.sample.footbot.model.livescore.LivescoreResponse;

import java.net.URI;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.integration.dsl.channel.MessageChannels.direct;
import static ru.toparvion.sample.footbot.flow.IntegrationConstants.LIVE_EVENTS_REQUEST_CHANNEL;
import static ru.toparvion.sample.footbot.flow.IntegrationConstants.LIVE_EVENTS_RESPONSE_CHANNEL;

/**
 * @author Toparvion
 * @since v0.0.1
 */
@Slf4j
@Configuration
@IntegrationComponentScan
public class SyncFlow {

  private final String apiEndpoint;
  private final String apiKey;
  private final String apiSecret;

  @Autowired
  public SyncFlow(@Value("${livescore.api.endpoint}") String apiEndpoint,
                  @Value("${livescore.api.key}") String apiKey,
                  @Value("${livescore.api.secret}") String apiSecret) {
    this.apiEndpoint = apiEndpoint;
    this.apiKey = apiKey;
    this.apiSecret = apiSecret;
  }

  @Bean
  public IntegrationFlow liveEventsFlow() {
    return IntegrationFlows
        .from(direct(LIVE_EVENTS_REQUEST_CHANNEL))
        .log()
        .handle(Http.outboundGateway(this::buildLivescoreUri)
            .httpMethod(GET)
            .expectedResponseType(new ParameterizedTypeReference<LivescoreResponse<Event>>(){}))
        .log()
        .channel(direct(LIVE_EVENTS_RESPONSE_CHANNEL))
        .get();
  }

  private URI buildLivescoreUri(Message<String> req) {
    URI uri = UriComponentsBuilder.fromUriString(apiEndpoint)
        .path("/scores/events.json")
        .queryParam("key", apiKey)
        .queryParam("secret", apiSecret)
        .queryParam("id", req.getPayload())
        .build()
        .toUri();
    log.info("Livescore API request URI: {}", uri);
    return uri;
  }
}
