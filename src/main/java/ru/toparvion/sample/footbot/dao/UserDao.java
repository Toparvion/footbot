package ru.toparvion.sample.footbot.dao;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import ru.toparvion.sample.footbot.model.db.BotUser;

import java.util.List;

@Repository
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class UserDao {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public void mergeUser(BotUser botUser) {
//    String sql = "MERGE INTO BOT_USERS KEY(USER_ID) VALUES(:userId, :userName, :level)";
    String sql =
      "INSERT INTO BOT_USERS VALUES(:userId, :userName, :level) " +
          "ON CONFLICT (USER_ID) " +
          "DO UPDATE " +
          " SET USER_ID=:userId, USER_NAME=:userName, LEVEL=:level";

    SqlParameterSource params = new BeanPropertySqlParameterSource(botUser);
    jdbcTemplate.update(sql, params);
  }

  public List<BotUser> fetchAllUsers() {
    String sql = "SELECT user_id as userId, user_name as userName, level FROM BOT_USERS";
    return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(BotUser.class));
  }
}
