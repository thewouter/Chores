package nl.wouter.chores;

import android.content.Context;

import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.rxjava2.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava2.RxDataStore;

public class DataStoreSingleton {
    RxDataStore<Preferences> datastore;
    private static final DataStoreSingleton ourInstance = new DataStoreSingleton();
    public static DataStoreSingleton getInstance() {
        return ourInstance;
    }
    private DataStoreSingleton() { }
    public void setDataStore(RxDataStore<Preferences> datastore) {
        this.datastore = datastore;
    }
    public RxDataStore<Preferences> getDataStore(Context context) {
        if (this.datastore == null) {
            this.datastore = new RxPreferenceDataStoreBuilder(context, "chores_settings").build();
        }
        return datastore;
    }
}