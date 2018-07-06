package ru.toparvion.sample.footbot.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static java.lang.String.format;
import static ru.toparvion.sample.footbot.util.IntegrationConstants.EVENT_ID_HEADER;
import static ru.toparvion.sample.footbot.util.IntegrationConstants.MATCH_ID_HEADER;

/**
 * @author Toparvion
 * @since v0.9
 */
@Slf4j
@Repository
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MessageDao {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public Optional<Integer> findEditableMessageId(String matchId, String eventId, Integer chatId) {
    String sql =
        "select MESSAGE_ID " +
            "from SENT_MESSAGES " +
            "where MATCH_ID = :matchId " +
            "  and EVENT_ID = :eventId " +
            "  and CHAT_ID = :chatId";
    SqlParameterSource params = new MapSqlParameterSource()
        .addValue("matchId", matchId)
        .addValue("eventId", eventId)
        .addValue("chatId", chatId);
    try {
      return Optional.ofNullable(
          jdbcTemplate.queryForObject(sql, params, Integer.class));

    } catch (EmptyResultDataAccessException e) {
      log.trace("Не найдено сохраненного ID сообщения для matchId={}, eventId={}, chatId={}", matchId, eventId, chatId);
      return Optional.empty();
    }
  }

  public String getInsertSql() {
    return format(
        "INSERT INTO SENT_MESSAGES VALUES (:headers[%s], :headers[%s], :payload.chat.id, :payload.messageId)",
        MATCH_ID_HEADER, EVENT_ID_HEADER);
  }
}
