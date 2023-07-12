package uk.gov.dwp.health.pip.identity.utils;

import uk.gov.dwp.health.pip.identity.exception.GenericRuntimeException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateParseUtil {

  private DateParseUtil() {}

  public static String dateTimeToString(LocalDateTime localDateTime) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    return localDateTime.format(formatter);
  }

  public static LocalDateTime stringToDateTime(String dateStamp) {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      return LocalDateTime.ofInstant(sdf.parse(dateStamp).toInstant(), ZoneId.systemDefault());
    } catch (ParseException pe) {
      throw new GenericRuntimeException("Invalid Date format");
    }
  }
}
