package nl.wouter.chores;

import android.content.Context;
import android.text.Spanned;

public abstract class Button {
    public static final int MS_TRIGGER_INTERVAL = 1000;
    public abstract boolean press(Context context);
    public abstract Spanned getText();
}
