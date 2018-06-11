package ru.toparvion.sample.footbot.flow;

import org.springframework.integration.annotation.MessagingGateway;
import ru.toparvion.sample.footbot.model.livescore.Event;
import ru.toparvion.sample.footbot.model.livescore.LivescoreResponse;

import static ru.toparvion.sample.footbot.flow.IntegrationConstants.LIVE_EVENTS_REQUEST_CHANNEL;
import static ru.toparvion.sample.footbot.flow.IntegrationConstants.LIVE_EVENTS_RESPONSE_CHANNEL;

/**
 * @author Toparvion
 * @since v0.0.1
 */
@MessagingGateway(defaultRequestChannel = LIVE_EVENTS_REQUEST_CHANNEL,
                  defaultReplyChannel = LIVE_EVENTS_RESPONSE_CHANNEL)
public interface LiveEventsGateway {

  LivescoreResponse<Event> fetchEvents(String matchId);
}
