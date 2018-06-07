package ru.toparvion.sample.footbot.model.livescore;

import lombok.Data;

/**
 * @author Toparvion
 * @since v0.9
 */
@Data
public class ResponseData<T> {
  T event[];
}
