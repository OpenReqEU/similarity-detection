package upc.similarity.compareapi.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import upc.similarity.compareapi.util.Time;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Execution implements Serializable {

    private String id;
    private String methodName;
    private Date startTime;
    private String spentTime;
    private Integer pagesLeft;

    public Execution(String id, String methodName, Integer pagesLeft, long startTime, Long finalTime) {
        this.id = id;
        this.methodName = methodName;
        Timestamp stamp = new Timestamp(startTime);
        this.startTime = new Date(stamp.getTime());
        this.pagesLeft = pagesLeft;
        if (finalTime == null) this.spentTime = computeSpentTime(startTime, Time.getInstance().getCurrentMillis());
        else this.spentTime = computeSpentTime(startTime, finalTime);
    }

    public String getId() {
        return id;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getSpentTime() {
        return spentTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Integer getPagesLeft() {
        return pagesLeft;
    }

    private String computeSpentTime(long startTime, long finalTime) {
        long time = finalTime - startTime;
        int days = (int) time/(24*60*60*1000);
        time = time%(24*60*60*1000);
        int hours = (int) time/(60*60*1000);
        time = time%(60*60*1000);
        int minutes = (int) time/(60*1000);
        time = time%(60*1000);
        int seconds = (int) time/(1000);
        time = time%1000;
        return spentTime = days + " days " + hours + " hours " + minutes + " minutes " + seconds + " seconds and " + time + " milliseconds //"+days+"-"+hours+"-"+minutes+"-"+seconds+"-"+time+"//";
    }
}
