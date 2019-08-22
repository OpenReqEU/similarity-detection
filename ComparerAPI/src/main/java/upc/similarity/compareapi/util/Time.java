package upc.similarity.compareapi.util;

import java.time.Clock;

public class Time {

    private static Time instance = new Time();
    private Clock clock;

    private Time() {
        clock = Clock.systemUTC();
    }

    public static Time getInstance() {
        return instance;
    }

    public long getCurrentMillis() {
        return clock.millis();
    }

    public Clock getClock() {
        return clock;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }
}
