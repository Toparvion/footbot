package ru.toparvion.sample.footbot.model.livescore;

import lombok.Data;

/**
 * @author Toparvion
 * @since v0.9
 */
@Data
public class LivescoreResponse<T> {
  boolean success;
  ResponseData<T> data;
}
