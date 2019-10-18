package upc.similarity.compareapi.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import upc.similarity.compareapi.util.Logger;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.util.Time;

@Component
public class ScheduledTasks {

    @Scheduled(cron = "0 0 0 1,7,14,21,28 * ?") //once a week
    public void deleteOldResponses() {
        Logger.getInstance().showInfoMessage("DeleteOldResponses: Start computing");
        try {
            long time = Time.getInstance().getCurrentMillis();
            long weekMillis = (long) (7 * 24 * 60 * 60 * 1000);
            DatabaseOperations.getInstance().deleteOldResponses(time - weekMillis);
        } catch (InternalErrorException e) {
            //does nothing
        }
        Logger.getInstance().showInfoMessage("DeleteOldResponses: Finish computing");
    }
}
