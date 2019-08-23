package upc.similarity.compareapi.entity;

import upc.similarity.compareapi.util.Time;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

public class Execution implements Serializable {

    private String id;
    private String methodName;
    private Date startTime;
    private String spentTime;
    private Integer pagesLeft;

    public Execution(String id, String methodName, Integer pagesLeft, long startTime) {
        this.id = id;
        this.methodName = methodName;
        Timestamp stamp = new Timestamp(startTime);
        this.startTime = new Date(stamp.getTime());
        this.pagesLeft = pagesLeft;
        long time = Time.getInstance().getCurrentMillis() - startTime;
        int days = (int) time/(24*60*60*1000);
        time = time/(24*60*60*1000);
        int hours = (int) time/(60*60*1000);
        time = time/(60*60*1000);
        int minutes = (int) time/(60*1000);
        time = time/(60*1000);
        int seconds = (int) time/(1000);
        this.spentTime = days + " days " + hours + " hours " + minutes + " minutes and " + seconds + " seconds //"+days+"-"+hours+"-"+minutes+"-"+seconds+"//";
    }

}
