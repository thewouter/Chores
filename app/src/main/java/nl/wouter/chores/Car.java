package nl.wouter.chores;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;

public class Car extends Button{
    private static final int REQUIRED_PRESSES = 2;
    private int car_km;
    private int presses;
    public static final int KM_DELTA = 10;
    private long pressed_time;
    public static final int KM_DELTA_DIRECTION = (Chore.DEFAULT_PRESSER == "W" ? +1 : -1);

    public Car(){
        this.car_km = 0;
        this.presses = 0;
        this.pressed_time = Calendar.getInstance().getTime().getTime();
    }

    public int getCar_km() {
        return car_km;
    }

    public void setCar_km(int car_km) {
        this.car_km = car_km;
    }

    public void add_car_km() {
        this.car_km += Car.KM_DELTA * Car.KM_DELTA_DIRECTION;
    }

    public Spanned getText() {
        return Html.fromHtml("\uD83D\uDE97 <br/> <small><small><small> " + this.car_km * Car.KM_DELTA_DIRECTION +
                "</small></small></small>", Html.FROM_HTML_MODE_LEGACY);
    }

    @Override
    public boolean press(Context context) {
        long currentTime = Calendar.getInstance().getTime().getTime();
        long delta = currentTime - this.pressed_time;
        Log.d("Widget", String.valueOf(delta));
        this.pressed_time = currentTime;

        if (delta - Button.MS_TRIGGER_INTERVAL < 0) {
            this.presses += 1;
        } else {
            this.presses = 1;
        }

        Log.d("Widget", String.valueOf(this.presses));
        if (this.presses >= Car.REQUIRED_PRESSES){
            Toast.makeText(context, "\uD83D\uDE97 + " + Car.KM_DELTA, Toast.LENGTH_SHORT).show();
            this.add_car_km();
            return true;
        }
        return false;
    }
}
