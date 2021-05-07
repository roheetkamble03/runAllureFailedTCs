import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeStamp {

   public static void main(String[] args){
       DateFormat dateFormat = new SimpleDateFormat("ddMMyyyy HH:mm:ss");
       Date date = new Date();
       Calendar calendar = Calendar.getInstance();
       calendar.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
       System.out.println(dateFormat.format(calendar.getTime()));
   }
}
