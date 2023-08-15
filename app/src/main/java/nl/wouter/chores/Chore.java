package nl.wouter.chores;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.Calendar;

public class Chore {
    private float reset_interval;
    private float countdown_time;
    private long pressed_time;
    private String name;
    private int presses;
    private static final int REQUIRED_PRESSES = 3;
    private static final int MS_TRIGGER_INTERVAL = 1000;
    private String presser = "";
    public static final String DEFAULT_PRESSER = "W";

    public Chore(){
        this.pressed_time = Calendar.getInstance().getTime().getTime();
    }

    public float getReset_interval() {
        return reset_interval;
    }

    public void setReset_interval(float reset_interval) {
        this.reset_interval = reset_interval;
    }

    public float getCountdown_time() {
        return countdown_time;
    }

    public void setCountdown_time(float countdown_time) {
        this.countdown_time = countdown_time;
    }

    public void setPressed_time(long pressed_time){
        this.pressed_time = pressed_time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPresser(String presser) {
        this.presser = presser;
    }

    public int getPresses() {
        return presses;
    }

    public Spanned getText() {
        return Html.fromHtml(this.name + " <br/> <small><small><small> " + this.presser +
                "</small></small></small>", Html.FROM_HTML_MODE_LEGACY);
    }

    public void resetCountdown_time() {
        this.countdown_time = this.reset_interval;
    }

    /**
     * Press the chore button, return whether to send the update
     * @return whether to mark the chore as done
     */
    public boolean press(Context context) {
        long currentTime = Calendar.getInstance().getTime().getTime();
        long delta = currentTime - this.pressed_time;
        Log.d(ChoresWidget.TAG, String.valueOf(delta));
        this.pressed_time = currentTime;
        if (delta - Chore.MS_TRIGGER_INTERVAL < 0) {
            this.presses += 1;
        } else {
            this.presses = 1;
        }
        Log.d(ChoresWidget.TAG, String.valueOf(this.presses));
        if (this.presses == Chore.REQUIRED_PRESSES){

            ChoresWidget.handler.post(() -> Toast.makeText(context, "Pressed " + getName(), Toast.LENGTH_SHORT).show());

            return true;
        }
        return false;
    }
}
