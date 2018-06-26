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
        "MERGE INTO BOT_USERS t " +
        "USING (VALUES(:userId, :userName, :level)) v " +
            "ON t.USER_ID = v.column1 " +
            "WHEN NOT MATCHED " +
            " INSERT VALUES(v.column1, v.column2, v.column3) " +
            "WHEN MATCHED " +
            " UPDATE SET USER_ID = v.column1, USER_NAME = v.column2, LEVEL = v.column3";

    String sqlExmaple =
        "MERGE INTO wines w\n" +
            "USING (VALUES('Chateau Lafite 2003', '24')) v\n" +
            "ON v.column1 = w.winename\n" +
            "WHEN NOT MATCHED \n" +
            "  INSERT VALUES(v.column1, v.column2)\n" +
            "WHEN MATCHED\n" +
            "  UPDATE SET stock = stock + v.column2;";
    SqlParameterSource params = new BeanPropertySqlParameterSource(botUser);
    jdbcTemplate.update(sql, params);
  }

  public List<BotUser> fetchAllUsers() {
    String sql = "SELECT user_id as userId, user_name as userName, level FROM BOT_USERS";
    return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>());
  }
}
