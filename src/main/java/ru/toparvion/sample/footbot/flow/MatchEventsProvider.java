package ru.toparvion.sample.footbot.flow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.toparvion.sample.footbot.model.config.Schedule;
import ru.toparvion.sample.footbot.model.config.ScheduledMatch;
import ru.toparvion.sample.footbot.model.sportexpress.Match;
import ru.toparvion.sample.footbot.model.sportexpress.event.Event;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static ru.toparvion.sample.footbot.util.IntegrationConstants.CURRENT_MATCH_SCORE_HEADER;

/**
 * Источник живых данных о происходящих играх
 */
@Service
@Slf4j
public class MatchEventsProvider implements MessageSource<List<Event>> {
  private static final int beforeStartGapMinutes = 25;
  private static final int afterStartGapMinutes = 160;

  private final Schedule schedule;
  private final String infoSourceUri;

  private final RestTemplate restTemplate;

  @Autowired
  public MatchEventsProvider(Schedule schedule,
                             @Value("${sportexpress.broadcastUri}") String broadcastUri) {
    this.schedule = schedule;
    this.infoSourceUri = broadcastUri;
    restTemplate = new RestTemplate();
  }

  @Override
  public Message<List<Event>> receive() {
    Optional<ScheduledMatch> scheduledMatchOpt = findActualScheduledMatch();
    if (!scheduledMatchOpt.isPresent()) {
      log.debug("Ни одного активного матча не найдено. Пропускаю дальнейшую обработку.");
      return null;
    }
    ScheduledMatch scheduledMatch = scheduledMatchOpt.get();
    log.debug("Запрашиваю события активного матча '{}' (id={}, {})", scheduledMatch.getTitle(), scheduledMatch.getId(),
            scheduledMatch.getDate());

    Match actingMatch = restTemplate.getForObject(infoSourceUri, Match.class, scheduledMatch.getId());
    if (actingMatch == null) {
      log.error("Не найден матч '{}' по id={}", scheduledMatch.getTitle(), scheduledMatch.getId());
      return null;
    }
    List<Event> matchEvents = actingMatch.getEvents();
    log.debug("Получено {} событий матча.", matchEvents.size());
    return MessageBuilder
            .withPayload(matchEvents)
            .setHeader(CURRENT_MATCH_SCORE_HEADER, actingMatch.getScore())
            .build();
  }

  private Optional<ScheduledMatch> findActualScheduledMatch() {
    ZonedDateTime now = ZonedDateTime.now();
    List<ScheduledMatch> matches = this.schedule.getSchedule();
    for (ScheduledMatch match: matches) {
      ZonedDateTime matchPeriodStart = match.getDate().minusMinutes(beforeStartGapMinutes);
      ZonedDateTime matchPeriodEnd = match.getDate().plusMinutes(afterStartGapMinutes);
      if (now.isAfter(matchPeriodStart) && now.isBefore(matchPeriodEnd)) {
        return Optional.of(match);
      }
    }
    return Optional.empty();
  }
}
