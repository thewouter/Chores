package nl.wouter.chores;

import java.util.Calendar;

public class Redo {
    public String name;
    public long time;

    public Redo(String name) {
        this.name = name;
        this.time = Calendar.getInstance().getTime().getTime();;
    }

    public long getTime() {
        return this.time;
    }

    public String getName() {
        return this.name;
    }
}
