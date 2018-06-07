package ru.toparvion.sample.footbot.flow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.webflux.dsl.WebFlux;
import org.springframework.messaging.Message;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import ru.toparvion.sample.footbot.model.livescore.Event;
import ru.toparvion.sample.footbot.model.livescore.LivescoreResponse;

import java.net.URI;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * @author Toparvion
 * @since v0.0.1
 */
@Configuration
@Slf4j
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
        .from(WebFlux.inboundGateway("/events")
            .requestMapping(m -> m.produces(APPLICATION_JSON_VALUE)
                                  .methods(GET)))
        .log()
        .handle(WebFlux.outboundGateway(this::buildLivescoreUri)
                       .httpMethod(GET)
                       .expectedResponseType(new ParameterizedTypeReference<LivescoreResponse<Event>>(){})
                       .replyPayloadToFlux(false))
//        .log()
        .get();
  }

  private URI buildLivescoreUri(Message<MultiValueMap<String, String>> req) {
    URI uri = UriComponentsBuilder.fromUriString(apiEndpoint)
        .path("/scores/events.json")
        .queryParam("key", apiKey)
        .queryParam("secret", apiSecret)
        .queryParam("id", req.getPayload().getFirst("matchId"))
        .build()
        .toUri();
    log.info("Livescore API request URI: {}", uri);
    return uri;
  }
}
