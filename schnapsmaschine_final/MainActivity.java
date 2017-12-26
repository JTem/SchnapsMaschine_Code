package com.example.jens.schnapsmaschine_final;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/*Soo hier ist die App. Zumindest der Grundriss.^^ mit der Hilfe eines netten Studenten aus Aachen ist der Fehler jetzt auch behoben.
* übers design und evt auftretenen Bugs die ich ncoh nicht gefunden hab kannst du ja deine Kritik äußern.
* evt. könnte man für die zukunft sogar noch kleine Trinkspiele implementieren oder so, wer weiß. naja dann fangen wir mal an.
* MainAktivity.java: hier ist alles was mit dem Programm zu tun hat
* Splash.java: das ist der Screen den man sieht wenn man die App öffnet.. das sieht nur cool aus hat sonst aber keinen Zweck
*
* die ganzen Bilder sind nur placeholder weil ich selbst noch keine angefertigt hab
*
* Du kannst alles nach lust und laune verändern evt kommt ja was gutes bei rum. Am meisten sorgen macht mir zur zeit das design^^ hab schon paar ideen aber keine Ahnung bzw keine lust die umzusetzen
*
* Designen ist echt ne scheißarbeit
*
* und ich seh auch gerade dass noch n paar bugs drin sind :D aber viel spass beim suchen ^^
* */

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity"; //TAG wird mit Log.d() zum debuggen benutzt.. das macht so texte untem im Logcat

    //GUI sachen die ich für die app brauche
    int counterData, minuteData;
    private Spinner spinnerCounter, spinnerMinute;

    //Bluetoothsachen etc
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice, mBTDevice;
    private BluetoothSocket mSocket = null;
    private final static String MY_UUID = "00001101-0000-1000-8000-00805f9b34fb";

    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTED = 3;
    public int mState;


    private Button btnLos, btnCon;

    private OutputStream mmOutStream;


    @Override
    //onCreate ist immer das erste was beim erzeugen einer Aktivity aufgerufen wird- also quasi der Konstrukter der Klasse
    //hier werden die ganzen sachen die man für die GUI so braucht erzeugt usw
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//das hier ist damit der hässliche titel oben aus der app weggeht wenn man mit Empty Activity anfängt z.b.
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // hier wird der BT adaper vom handy erfasst und erzeugt. Man beachte dass man Permissions ins manifest einbringen muss damit man das tun kann

        //buttons. das layout ist dann auch in /app/res/layout/activity_main.xml zu sehen
        btnLos = (Button) findViewById(R.id.btnSend);
        btnCon = (Button) findViewById(R.id.btnConnect);



        //spinner sind die auswahldinger.. auch in activity_main
        spinnerCounter = (Spinner) findViewById(R.id.spC);
        spinnerMinute = (Spinner) findViewById(R.id.spM);

        //hier wird der inhalt der spinner erzeugt und mit setAdapter dann den spinnern zugewiesen. der schriftliche inhalt ist in /res/values/strings.xml zu finden
        //der style in /res/layout/spinner_item.xml
        ArrayAdapter<CharSequence> adapterMinutes = ArrayAdapter.createFromResource(this,
                R.array.spinner_Minutes, R.layout.spinner_item);
        ArrayAdapter<CharSequence> adapterCounter = ArrayAdapter.createFromResource(this,
                R.array.spinner_Counter, R.layout.spinner_item);

        spinnerCounter.setAdapter(adapterCounter);
        spinnerMinute.setAdapter(adapterMinutes);

        //das hier damit man die einträge auswählen kann
        spinnerCounter.setOnItemSelectedListener(new spinner_CounterClass());//hier kommen die Unter-Klassen zum einsatz- sonst einfach MainActivity.this()
        spinnerMinute.setOnItemSelectedListener(new spinner_MinuteClass());

        //mState ist damit man weiß ob man verbunden ist oder nicht. hier in dem punkt ist man noch nicht verbunden
        mState = STATE_NONE;

        //falls arduino und handy schon gepaired sind wird direkt verbunden
        //falls nicht gehts weiter
        tryToConnect();
        enableDisableBT(); //falls das BT aus ist wird hier angemacht

        if (mState != STATE_CONNECTED)
            discoverDevice(); // falls keine verbindung zustandegekommen ist wird auf discovery geschaltet




    }

    // tryToConnect: hier wird die eigentliche bt verbindung erzeugt
    //eigentlich wird nur ein gemeinsamer Socket erzeugt auf dem sich dann beide geräte verbinden und dann connected sind. falls das nicht gelingt
    //(evt. NullPointer weil das gerät offline ist oder so) gehts in die Catch und mState geht wieder auf STATE_NONE
    //falls es klappt dann sind beide geräte verbunden und können kommunizieren und mState geht auf STATE_CONNECTED
    public void tryToConnect() {

        try {

            findBTDevice();
            UUID uuid = UUID.fromString(MY_UUID);
            mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
            mSocket.connect();
            //Log.d(TAG, "connection SUCCESS!");
            btnCon.setVisibility(View.INVISIBLE);
            Animation myanim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.myfading3);
            Animation myanim2 = AnimationUtils.loadAnimation(MainActivity.this, R.anim.myfading2);
            btnCon.startAnimation(myanim);
            btnLos.setText("LOS");
            btnLos.setBackgroundResource(R.drawable.buttonstyle1);
            btnLos.startAnimation(myanim2);
            mState = STATE_CONNECTED;
            mmOutStream = mSocket.getOutputStream();
            byte[]data = "a".getBytes();
            mmOutStream.write(data);
            Log.d(TAG, "datalänge: "+data.length);


        } catch (Exception e) {
            //Log.d(TAG, "connectiontry failed");
            mState = STATE_NONE;
        }

    }

    //das ist eigentlich auch nur zum debuggen. das zeigt an wie der BT status ist wenn man es ein oder ausschaltet.. also eigetlich unwichtig
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                       // Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        //Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        //Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        discoverDevice();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                       // Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    //der hier ist wichtig: wenn ein device discovered wird dann springt dieser BroadcastRec an mit ACTION.FOUND
    //das device wird dann genommen, und der Name abgeglichen. falls das dinge den richtigen namen hat wird gepaired
    //und der connectThread wird gestartet(dazu unten mehr)
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            //Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                try {
                    //Log.d(TAG, "versuche mit SchnapsMaschine zu verbinden");
                    if (device.getName().equals("SchnapsMaschine")) {
                        mBluetoothAdapter.cancelDiscovery();


                        //Log.d(TAG, "createBond: trying to bond to device");


                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            mBTDevice = device;
                            mBTDevice.createBond();
                            //Log.d(TAG, "createBond: BOND_BONDED");
                            tryToConnect();
                        }

                    }

                    //hier die catch ist wichtig. falls er kein device finden kann innerhalb von ungefähr 4 sec oder so gehts in die catch und
                    //der VERBINDE button wird sichtbar gemacht damit man sich wieder verbinden kann. der sucht nämlich nicht ewig
                } catch (NullPointerException e) {
                   // Log.d(TAG, "createBond: no such device found");

                }

                //Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
            }
        }
    };


    //das ist die Methode die vom VERBINDE button aufgerufen wird. mit onClick (kann man in den .xml files sehen)
    public void connect(View view) {
        //Log.d(TAG, "connectButton pressed");
        enableDisableBT();
        discoverDevice();


    }

    //hier wird das richtige device von allen gepairedten devices rausgesucht damit man damit connecten kann
    private void findBTDevice() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals("SchnapsMaschine"))
                this.mDevice = device;
        }
    }

    //hier ist die discoveryMethode die in onCreate aufgerufen wird.
    public void discoverDevice() {
        //Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            //Log.d(TAG, "btnDiscover: Canceling discovery.");

            //check BT permissions im manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent); // hier gehts dann in der Broadcastreceiver3
        }
        if (!mBluetoothAdapter.isDiscovering()) {

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);

        }
    }

    //irgendwie muss man auch sowas haben das hat wohl mit SDK versionen zu tun oder was weiß ich
    private void checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        } else {
           // Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    //hier wird das BT an gemacht falls es aus ist. wenn das gerät keinen BT adapter hat dann wird n kleiner Log.d() ausgeschmissen sonst nichts
    public void enableDisableBT() {
        if (mBluetoothAdapter == null) {
           // Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            //Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent); // hier gehts dann wieder in der BroadcastReceiver damit man checken kann ob alles richtig läuft
        }

    }

    //Hier sind die hilfsklassen für die onItemSelctedListener. wenn man mehr als einen Spinner in einer Activity hat muss man das so lösen
    //weil sich die gewählten sachen sonst überschreiben würden
    private class spinner_CounterClass implements AdapterView.OnItemSelectedListener {
        //automatisch generierte Methoden wegen des implements
        public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {


            //switch case schien mir die einfachste Lösung zu sein..
            switch (position + 1) {
                case 1:
                    counterData = 12;
                    break;
                case 2:
                    counterData = 11;
                    break;
                case 3:
                    counterData = 10;
                    break;
                case 4:
                    counterData = 9;
                    break;
                case 5:
                    counterData = 8;
                    break;
                case 6:
                    counterData = 7;
                    break;
                case 7:
                    counterData = 6;
                    break;
                case 8:
                    counterData = 5;
                    break;
                case 9:
                    counterData = 4;
                    break;
                case 10:
                    counterData = 3;
                    break;
                case 11:
                    counterData = 2;
                    break;
                case 12:
                    counterData = 1;
                    break;
            }
           // Log.d(TAG, "Du hast so viele Schaps gewählt:" + counterData);
        }

        //hier die Methode wird auch automatisch erzeugt weil man halt AdapterView.OnItemSelectedListener erzeugt deswegen bleibt die einfach leer
        //weg machen kann man die glaub ich nicht ohne dass es zu Fehlern führt
        //wie ich aber bemerkt habe ist immer was ausgewählt also .. ja
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }

    //hier die zweite für die minutenauswahl
    private class spinner_MinuteClass implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {


            switch (position + 1) {

                case 1:
                    minuteData = 2;
                    break;
                case 2:
                    minuteData = 5;
                    break;
                case 3:
                    minuteData = 8;
                    break;
                case 4:
                    minuteData = 10;
                    break;
                case 5:
                    minuteData = 15;
                    break;
                case 6:
                    minuteData = 20;
                    break;
                case 7:
                    minuteData = 25;
                    break;
                case 8:
                    minuteData = 30;
                    break;
                case 9:
                    minuteData = 40;
                    break;
                case 10:
                    minuteData = 0;
                    break;


            }
            //Log.d(TAG, "Der Intervall beträgt:" + minuteData);
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }

    private String normalizeData(int m, int c) {

        //kleiner algorithmus dass man immer gleich lange Datenblöcke hat.. habs vorher mit Binärcode und Hexadezimal versucht aber das wird anders dargestellt als ich dachte.
        //so muss der String im arduino programm einfach nur wieder in Integer gecastet und 100 abgezogen werden
        String min = Integer.toString(m + 100);
        String count = Integer.toString(c + 100);

        String data = "S-" + min + "-" + count + "-E";
        return data;
    }

    //hier werden die Daten gesendet. alles wird erst in Byte umgewandelt und dann durch den mmOutStream an den arduino gesendet. auch alles mit Try- catch
    //weil das nur mit ner laufenden connection funktioniert. gehts in die catch ist mState = STATE_NONE
    public void sendData(View view) {

       // Log.d(TAG, "sendData: " + normalizeData(minuteData, counterData));
        byte[] data = normalizeData(minuteData, counterData).getBytes();

        //Log.d(TAG, "dataBytes :" + data.length);
        try {
            mmOutStream.write(data);
        } catch (IOException e) {
           // Log.e(TAG, "write: Error writing to output stream. " + e.getMessage());
            btnCon.setVisibility(View.VISIBLE);
            Animation myanim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.myfading3);
            Animation myanim2 = AnimationUtils.loadAnimation(MainActivity.this, R.anim.myfading2);
            btnCon.startAnimation(myanim2);
            btnLos.setText("Verbindung Verloren");
            btnLos.setBackgroundResource(R.drawable.buttonstyle2);
            //btnLos.startAnimation(myanim);
            mState = STATE_NONE;
        } catch (NullPointerException e) {
            mState = STATE_NONE;
        }
    }
}
