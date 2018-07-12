package ru.toparvion.sample.footbot.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import ru.toparvion.sample.footbot.model.db.BotUser;
import ru.toparvion.sample.footbot.model.sportexpress.event.Type;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonMap;

@Repository
public class UserDao {

  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final boolean isH2enabled;

  @Autowired
  public UserDao(NamedParameterJdbcTemplate jdbcTemplate,
                 @Value("${spring.h2.console.enabled:false}") boolean isH2enabled) {
    this.jdbcTemplate = jdbcTemplate;
    this.isH2enabled = isH2enabled;
  }

  public void saveUser(BotUser botUser) {
    // грязный хак для учета различий в синтаксисах SQL в H2 и PostgreSQL
    String sql = isH2enabled
        ? "MERGE INTO BOT_USERS KEY(USER_ID) VALUES(:userId, :userName, :level, current_date)"
        : "INSERT INTO BOT_USERS VALUES(:userId, :userName, :level, current_date) " +
          "       ON CONFLICT (USER_ID) " +
          "          DO UPDATE " +
          "             SET USER_ID=:userId, USER_NAME=:userName, LEVEL=:level, UPDATED=current_date";

    SqlParameterSource params = new BeanPropertySqlParameterSource(botUser);
    jdbcTemplate.update(sql, params);
  }

  public void updateUserLevel(int userId, Type newLevel) {
    String sql =
        "UPDATE bot_users" +
        " SET LEVEL=:newLevel" +
        " WHERE USER_ID=:userId";

    SqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", String.valueOf(userId))
        .addValue("newLevel", newLevel.name());
    jdbcTemplate.update(sql, params);
  }

  public List<BotUser> fetchAllUsers() {
    String sql = "SELECT user_id as userId, user_name as userName, level FROM BOT_USERS";
    return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(BotUser.class));
  }

  public Optional<Type> fetchUserLevel(Long chatId) {
    String sql =
        "select level " +
            "from bot_users " +
            "where user_id = :userId";
    try {
      return Optional.ofNullable(
          jdbcTemplate.queryForObject(sql, singletonMap("userId", String.valueOf(chatId)), Type.class));

    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }

  }
}
