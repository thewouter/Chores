package nl.wouter.chores;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.rxjava2.RxDataStore;

import java.util.Map;

import io.reactivex.Single;


public class DataStoreHelper {
//    Activity activity;
    RxDataStore<Preferences> dataStoreRX;
    Preferences pref_error = new Preferences() {
        @Nullable
        @Override
        public <T> T get(@NonNull Key<T> key) {
            return null;
        }

        @Override
        public <T> boolean contains(@NonNull Key<T> key) {
            return false;
        }

        @NonNull
        @Override
        public Map<Key<?>, Object> asMap() {
            return null;
        }
    };

    public DataStoreHelper(RxDataStore<Preferences> dataStoreRX) {
//        this.activity = activity;
        this.dataStoreRX = dataStoreRX;
    }

    public boolean putStringValue(String key, String value){
        boolean returnvalue;
        Preferences.Key<String> PREF_KEY = PreferencesKeys.stringKey(key);
        Single<Preferences> updateResult =  dataStoreRX.updateDataAsync(prefsIn -> {
            MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
            mutablePreferences.set(PREF_KEY, value);
            return Single.just(mutablePreferences);
        }).onErrorReturnItem(pref_error);
        returnvalue = updateResult.blockingGet() != pref_error;
        return returnvalue;
    }
    String getStringValue(String key) {
        Preferences.Key<String> PREF_KEY = PreferencesKeys.stringKey(key);
        Single<String> value = dataStoreRX.data().firstOrError().map(prefs -> prefs.get(PREF_KEY)).onErrorReturnItem("");
        return value.blockingGet();
    }

}