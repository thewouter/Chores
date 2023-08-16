package nl.wouter.chores;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ChoresWidgetConfigurationActivity extends Activity {
    static String CONFIG_TEST = "config_test";
    static String CHORES_LIST = "chores_list";
    static String GROUP_NAME = "group_name";

    int appWidgetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        DataStoreSingleton dataStoreSingleton = DataStoreSingleton.getInstance();
        DataStoreHelper dataStoreHelper = new DataStoreHelper(dataStoreSingleton.getDataStore(getApplicationContext()));

        setContentView(R.layout.configuration_widget);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // make the result intent and set the result to canceled
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        Button setupWidget = findViewById(R.id.button_start);
        EditText groupName = findViewById(R.id.group_name);
        groupName.setText(dataStoreHelper.getStringValue(ChoresWidgetConfigurationActivity.GROUP_NAME));

        setupWidget.setOnClickListener(view -> {
            dataStoreHelper.putStringValue(GROUP_NAME, groupName.getText().toString());
            setResult(RESULT_OK, resultValue);
            Intent reloadIntent = new Intent(this, ChoresWidget.class);
            reloadIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), ChoresWidget.class));
            reloadIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(reloadIntent);
            finish();
        });



        Log.d(ChoresWidget.TAG, String.valueOf(appWidgetId) + " aaaaaaaaa");
    }
}
