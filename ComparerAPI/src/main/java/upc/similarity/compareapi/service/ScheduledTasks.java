package upc.similarity.compareapi.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import upc.similarity.compareapi.config.Control;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.util.DatabaseOperations;
import upc.similarity.compareapi.util.Time;

@Component
public class ScheduledTasks {

    @Scheduled(cron = "0 0 0 0,7,14,21,28 * ?") //once a week
    public void deleteOldResponses() {
        Control.getInstance().showInfoMessage("DeleteOldResponses: Start computing");
        try {
            long time = Time.getInstance().getCurrentMillis();
            long weekMillis = 7 * 24 * 60 * 60 * 1000;
            DatabaseOperations.getInstance().deleteOldResponses(time - weekMillis);
        } catch (InternalErrorException e) {
            //does nothing
        }
        Control.getInstance().showInfoMessage("DeleteOldResponses: Finish computing");
    }
}
