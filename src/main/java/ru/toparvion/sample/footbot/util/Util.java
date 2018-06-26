package ru.toparvion.sample.footbot.util;

import com.vdurmont.emoji.EmojiParser;

import java.util.regex.Pattern;

/**
 * @author Toparvion
 * @since v0.0.1
 */
public final class Util {
  private static final Pattern DOUBLE_COLON_PATTERN = Pattern.compile("::(\\w+)::");

  public static String nvls(String val, String def) {
    return ((val != null) && !val.isEmpty())
        ? val
        : def;
  }

  public static String convertEmojies(String input) {
    String uncoloned = DOUBLE_COLON_PATTERN.matcher(input).replaceAll(":$1:");
    return EmojiParser.parseToUnicode(uncoloned);
  }

}
