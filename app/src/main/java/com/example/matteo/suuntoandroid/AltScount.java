package org.ambit.altscount.altscountandroid;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.ambit.altscount.altscountandroid.org.ambit.usb.android.AmbitManager;
import org.ambit.altscount.altscountandroid.org.ambit.usb.android.DeviceFactory;
import org.ambit.data.AmbitInfo;
import org.ambit.data.LogInfo;
import org.ambit.usb.UsbException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class AltScount extends ActionBarActivity {

    private AmbitManager ambitManager;
    private UsbManager usbManager;

    private List<String> moves = new ArrayList<String>();

    private ListView listViewMoves;

    private List<LogInfo> logInfos;
    File exportDir = new File("/sdcard/Download/");

    private  ArrayAdapter<String> movesAdpater;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            TextView myTextView = (TextView)findViewById(R.id.textView);
            final Button exButton = (Button) findViewById(R.id.exportBtn);
            exButton.setEnabled(true);
            myTextView.setText("Moves Exported");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alt_scount);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        // Init
        this.ambitManager = new AmbitManager(usbManager, this.getApplicationContext());

        /// The checkbox for the each item is specified by the layout android.R.layout.simple_list_item_multiple_choice
        movesAdpater = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, moves);

        // Getting the reference to the listview object of the layout
        listViewMoves = (ListView) findViewById(R.id.lvMoves);

        // Setting adapter to the listview
        listViewMoves.setAdapter(movesAdpater);

        Button button = (Button) findViewById(R.id.searchBtn);

        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v)
            {
                doSearch();
            }
        });

        final Button exButton = (Button) findViewById(R.id.exportBtn);

        exButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                SparseBooleanArray checked = listViewMoves.getCheckedItemPositions();

                TextView text = (TextView) findViewById(R.id.textView);
                text.setText("Exporting " + checked.size() + " moves...");
                exButton.setEnabled(false);

                Runnable runnable = new Runnable()
                {
                    public void run()
                    {
                        doExport();
                        handler.sendEmptyMessage(0);
                    }
                };

                Thread mythread = new Thread(runnable);
                mythread.start();
            }
        });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_alt_scount, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void doSearch() {

        TextView text = (TextView) findViewById(R.id.textView);
        text.setText("Searching...");

        movesAdpater.notifyDataSetChanged();


        try {
            ambitManager.connectDevice();


            Log.i("AUB","Before charge" );
            //ambitManager.getSettings2();

            //ambitManager.getDeviceInfo();
            //ambitManager.getSettings();

            short charge = ambitManager.getDeviceCharge();
            Log.i("AUB","Charge " + charge);

            /*AmbitInfo ambitInfo = ambitManager.getDeviceInfo();
            if ( ambitInfo != null ) {
                Log.i("AUB","ambitInfo Model " + ambitInfo.getModel());
            }
            else {
                Log.i("AUB","ambitInfo NULL ");
            }*/

            logInfos = ambitManager.readMoveDescriptions();

            for ( LogInfo logInfo : logInfos ) {
                Log.i("AUB","logInfo " + logInfo.getCompleteName() );
                moves.add(logInfo.getCompleteName() + " " + logInfo.getDistance() + "km");
            }

            Button button = (Button) findViewById(R.id.searchBtn);
            button.setVisibility(View.GONE);

        } catch (UsbException e) {
            Log.e("AUB", "Error " + e);
        }

        text.setText("Waiting...");
    }

    private void doExport()
    {
        long currentProgress = 0;
        long maxStep = 0;

        SparseBooleanArray checked = listViewMoves.getCheckedItemPositions();

        for (int i = 0; i < checked.size(); i++)
        {
            int key = checked.keyAt(i);
            boolean value = checked.get(key);

            if(value) {
                LogInfo info = (LogInfo) logInfos.get(key);
                maxStep += info.getSampleCount();
            }
        }

        for (int i = 0; i < checked.size(); i++)
        {
            int key = checked.keyAt(i);
            boolean value = checked.get(key);

            if (value)
            {
                LogInfo info = (LogInfo) logInfos.get(key);
                try
                {
                    ambitManager.exportLog(info, exportDir, currentProgress, maxStep);
                    currentProgress += info.getSampleCount();
                } catch (Exception ex)
                {

                }
            }
        }

    }
}
