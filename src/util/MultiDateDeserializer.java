package util;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class MultiDateDeserializer implements JsonDeserializer<Date> {

    // Danh sách các format cũ mà bạn đã lưu vào DB
    private static final String[] DATE_FORMATS = new String[] {
            "EEE MMM dd HH:mm:ss z yyyy",        // Tue Oct 07 22:40:17 GMT+07:00 2025
            "EEE MMM dd HH:mm:ss 'ICT' yyyy",    // Fri Nov 14 08:14:19 ICT 2025
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",      // ISO 8601 (MongoDB chuẩn)
            "yyyy-MM-dd"                         // dd-mm-yyyy dạng BOD
    };

    @Override
    public Date deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {

        String dateStr = json.getAsString();

        for (String format : DATE_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);
                sdf.setLenient(true);
                return sdf.parse(dateStr);
            } catch (ParseException ignored) {}
        }

        throw new JsonParseException("Không parse được ngày: " + dateStr);
    }
}
