package ru.toparvion.sample.footbot.util;

/**
 * @author Toparvion
 * @since v0.0.1
 */
public final class Util {

  public static String nvls(String val, String def) {
    return ((val != null) && !val.isEmpty())
        ? val
        : def;
  }

}
