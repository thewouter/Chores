package nl.wouter.chores;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.datastore.preferences.core.Preferences;
import android.nfc.Tag;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.content.SharedPreferences;

import androidx.datastore.rxjava2.RxDataStore;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Implementation of App Widget functionality.
 */
public class ChoresWidget extends AppWidgetProvider {
    private final OkHttpClient client = new OkHttpClient();
    public static final String TAG = "Widget_chores";
    RxDataStore<Preferences> dataStore;
    public static Handler handler;
    SharedPreferences sharedPreferences;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Thread thread = new Thread(() -> {
            try  {
                loadData(context);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    protected PendingIntent getPendingSelfIntent(Context context, String action, int button_id) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action + "_chore");
        intent.putExtra("buttonId", button_id);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_MUTABLE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        handler = new Handler();
        Log.d(TAG, "onReceive " + intent.getAction());
        if (intent.getAction().contains("newdata")){
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, ChoresWidget.class));
            reloadWidget(context, appWidgetManager, appWidgetIds);
        } else if (intent.getAction().equals("reload_chores")) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, ChoresWidget.class));
            onUpdate(context, appWidgetManager, appWidgetIds);

        } else if (intent.getAction().contains("chore")) {
            Log.d(TAG, intent.getAction());
            Log.d(TAG, String.valueOf(intent.getIntExtra("buttonId", -1)));
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.chores_widget);
            Log.d(TAG, "initiated views");
            Log.d(TAG, "set Text");
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, ChoresWidget.class));
            appWidgetManager.updateAppWidget(appWidgetIds, views);
            String name = intent.getAction().substring(0, intent.getAction().length() - 6);
            Log.d(TAG, name);
            if (Status.chores.size() == 0) {

                Gson gson = new Gson();
                DataStoreSingleton dataStoreSingleton = DataStoreSingleton.getInstance();
                DataStoreHelper dataStoreHelper = new DataStoreHelper(dataStoreSingleton.getDataStore(context));
                String json = dataStoreHelper.getStringValue(ChoresWidgetConfigurationActivity.CHORES_LIST);
                Log.d(TAG, "loaded: " + dataStoreHelper.getStringValue(ChoresWidgetConfigurationActivity.CHORES_LIST));
                Type listType = new TypeToken<ArrayList<Chore>>(){}.getType();
                Status.chores = gson.fromJson(json, listType);
                Log.d(TAG, "reinitiated chores from datastore: " + Status.chores.toString());
            }
            Log.d(TAG, Status.chores.toString());
            handlePress(context, name);
            reloadWidget(context, appWidgetManager, appWidgetIds);
        }
        super.onReceive(context, intent);
    }

    private void handlePress(Context context, String name, long time){
        Chore chore = handlePress(context, name);
        if (chore != null) {
            chore.setPressed_time(time);
        }
    }

    private Chore handlePress(Context context, String name){
        DataStoreSingleton dataStoreSingleton = DataStoreSingleton.getInstance();
        DataStoreHelper dataStoreHelper = new DataStoreHelper(dataStoreSingleton.getDataStore(context));

        for (Chore chore : Status.chores) {
            if (chore.getName().equals(name)) {
                if (!chore.press(context)){
                    Log.d(TAG, "Press failed time " + chore.getPresses());
                    break;
                }
                Log.d(TAG, "Press passed");
                chore.resetCountdown_time();
                chore.setPresser(Chore.DEFAULT_PRESSER);
                saveChores(context);

                Thread thread = new Thread(() -> {
                    try  {
                        makePost(name, dataStoreHelper.getStringValue(ChoresWidgetConfigurationActivity.GROUP_NAME));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                thread.start();

                return chore;
            }
            Log.d(TAG, "No match for " + chore.getName() + " with " + name);
        }
        return null;
    }

    private void loadData(Context context) throws IOException {
        Log.d(TAG, "Loading data via API");
        DataStoreSingleton dataStoreSingleton = DataStoreSingleton.getInstance();
        DataStoreHelper dataStoreHelper = new DataStoreHelper(dataStoreSingleton.getDataStore(context));
        String name = dataStoreHelper.getStringValue(ChoresWidgetConfigurationActivity.GROUP_NAME);
        String url = "https://radixenschede.nl/wouter/chores/index.php" + (name.equals("") ? "" : "?group_name="+name);
        Log.d(TAG, url);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        String data;
        try (Response response = client.newCall(request).execute()) {
            Log.d(TAG, "Load");
            assert response.body() != null;
            data = response.body().string();
        }
        data = data.substring(5); // Fix for bug server side.
        Log.d(TAG, data);
        try {
            JSONArray jsonArray = new JSONArray(data);
            ArrayList<Chore> chores = new ArrayList<>();
            for(int i = 0 ; i < jsonArray.length(); i ++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Chore chore = new Chore();
                chore.setReset_interval((float) jsonObject.getDouble("reset_interval"));
                chore.setCountdown_time((float) jsonObject.getDouble("countdown_time"));
                chore.setPresser(jsonObject.getString("presser"));
                chore.setName(jsonObject.getString("name"));
                chores.add(chore);
            }
            if (chores.size() > 0) {
                Status.chores = chores;
                Log.d(TAG, String.valueOf(Status.redoList));
                Status.updating = false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Loaded");

//        Save the newly loaded chores list to the DataStore
        saveChores(context);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, ChoresWidget.class));
        reloadWidget(context, appWidgetManager, appWidgetIds);
    }

    private void saveChores(Context context){
        Gson gson = new Gson();
        String json = gson.toJson(Status.chores);
        DataStoreSingleton dataStoreSingleton = DataStoreSingleton.getInstance();
        DataStoreHelper dataStoreHelper = new DataStoreHelper(dataStoreSingleton.getDataStore(context));
        dataStoreHelper.putStringValue(ChoresWidgetConfigurationActivity.CHORES_LIST, json);
        Log.d(TAG, "Saved chores to DataStore");
    }

    private void makePost(String name, String group_name){
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("name", name)
                .addFormDataPart("presser", Chore.DEFAULT_PRESSER)
                .addFormDataPart("group_name", group_name)
                .build();

        Request request = new Request.Builder()
                .url("https://radixenschede.nl/wouter/chores/index.php")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            Headers responseHeaders = response.headers();
            for (int i = 0; i < responseHeaders.size(); i++) {
                System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
            }

            Log.d(TAG, response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadWidget(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            Log.d(TAG, "Appwidgetid: " + appWidgetId);
            // Construct the RemoteViews object
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.chores_widget);

            Intent reloadIntent = new Intent("reload_chores", null, context, ChoresWidget.class);
            views.setOnClickPendingIntent(R.id.grid_layout, PendingIntent.getBroadcast(context, 999, reloadIntent, PendingIntent.FLAG_IMMUTABLE));

            int counter = 1;
            Log.d(TAG, "start adding chores");
            if (Status.chores.size() < 1) {
                Log.d(TAG, "no chores found");
                Gson gson = new Gson();
                DataStoreSingleton dataStoreSingleton = DataStoreSingleton.getInstance();
                DataStoreHelper dataStoreHelper = new DataStoreHelper(dataStoreSingleton.getDataStore(context));
                String json = dataStoreHelper.getStringValue(ChoresWidgetConfigurationActivity.CHORES_LIST);
                Log.d(TAG, "loaded: " + dataStoreHelper.getStringValue(ChoresWidgetConfigurationActivity.CHORES_LIST));
                Status.chores = gson.fromJson(json, ArrayList.class);

            }
            for (Chore chore : Status.chores) {
                Log.d(TAG, "initiating " + chore.getName());
                int button_id = context.getResources().getIdentifier("button_" + counter, "id", context.getPackageName());
                Log.d(TAG, String.valueOf(button_id));
                String name_color = String.format(Locale.ENGLISH, "rg_%02d", (int) ((100 * chore.getCountdown_time()) / chore.getReset_interval()));
                int color_id = context.getResources().getIdentifier(name_color, "color", context.getPackageName());
                Log.d(TAG, name_color);
                views.setColor(button_id, "setBackgroundColor", color_id);
                views.setTextViewText(button_id, chore.getText());
                views.setOnClickPendingIntent(button_id, getPendingSelfIntent(context, chore.getName(), button_id));
                views.setViewVisibility(button_id, View.VISIBLE);
                counter++;
            }
            for (; counter <= 16; counter++) {
                int button_id = context.getResources().getIdentifier("button_" + counter, "id", context.getPackageName());
                Log.d(TAG, String.format("Hidden %d", counter));
                views.setViewVisibility(button_id, View.INVISIBLE);
            }
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

    }

    @Override
    public void onEnabled(Context context) {
        PeriodicWorkRequest widgetUpdateRequest = new PeriodicWorkRequest.Builder(WidgetUpdateWorker.class, 20, TimeUnit.MINUTES)
                .addTag("WidgetUpdateWorker")
                .setInitialDelay(30, TimeUnit.SECONDS)
                .build();
        WorkManager workManager = WorkManager.getInstance(context);
        workManager.enqueueUniquePeriodicWork(
                "WidgetUpdateWork",
                ExistingPeriodicWorkPolicy.REPLACE,
                widgetUpdateRequest);
        Log.d(TAG, "Started worker update request");
    }

    @Override
    public void onDisabled(Context context) {
        WorkManager workManager = WorkManager.getInstance(context);
        workManager.cancelAllWorkByTag("WidgetUpdateWorker");
    }
}