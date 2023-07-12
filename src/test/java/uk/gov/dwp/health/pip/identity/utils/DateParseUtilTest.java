package uk.gov.dwp.health.pip.identity.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.pip.identity.exception.GenericRuntimeException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class DateParseUtilTest {

    @Test
    void dateTimeToString_formatsDate_whenGivenValidDateAtMidnight() {
        LocalDateTime dateTime = LocalDate.of(2020, 1, 8).atStartOfDay();
        String expectedString = "2020-01-08 00:00:00";

        String actualString =  DateParseUtil.dateTimeToString(dateTime);
        assertEquals(expectedString, actualString);
    }

    @Test
    void dateTimeToString_formatsDate_whenGivenValidDateAtRandomTime() {
        LocalDateTime dateTime = LocalDate.of(2017, 5, 20).atTime(17, 50, 11);
        String expectedString = "2017-05-20 17:50:11";

        String actualString =  DateParseUtil.dateTimeToString(dateTime);
        assertEquals(expectedString, actualString);
    }

    @Test
    void stringToDateTime_convertsToDateTime_whenGivenValidString() {
        LocalDateTime expectedDateTime = LocalDate.of(2020, 1, 8).atStartOfDay();
        String stringToConvert = "2020-01-08 00:00:00";

        LocalDateTime actualDateTime =  DateParseUtil.stringToDateTime(stringToConvert);
        assertEquals(expectedDateTime, actualDateTime);
    }

    @Test
    void stringToDateTime_throwsGenericRuntimeException_whenGivenInValidString() {
        String stringToConvert = "An invalid string";
        assertThrows(GenericRuntimeException.class, () -> DateParseUtil.stringToDateTime(stringToConvert));
    }
}
