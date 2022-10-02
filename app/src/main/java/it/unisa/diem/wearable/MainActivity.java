package it.unisa.diem.wearable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import it.unisa.diem.wearable.communication.MQTTClient;
import it.unisa.diem.wearable.sensor.AccelerometerHandler;
import it.unisa.diem.wearable.sensor.LocationHandler;
import it.unisa.diem.wearable.sensor.OrientationHandler;

public class MainActivity extends AppCompatActivity {


    private String deviceID;
    private String username, password;
    private String broadcastTopic;
    private MQTTClient mqttClient;
    private boolean mqttClientConnected;

    private DecimalFormat decimalFormat;

    private AccelerometerHandler accelerometerHandler;
    private OrientationHandler orientationHandler;
    private LocationHandler locationHandler;

    private SensorManager sensorManager;
    private SensorEventListener sensorListener;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private final int LOCATION_REQUEST_CODE = 100;

    private Sensor accelerometerSensor, orientationSensor;

    private int samplingPeriod;
    private boolean accelerometer, orientation, location;

    /**
     * This method is called when the app is opened.
     * It creates instances of MQTT Client through the broker URI written in the "configuration.xml" resource,
     * Sensor Manager and Location Manager, generates a random ID for the device
     * and reads the default values for configuration variables from the resource.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Creation of a decimal format
        decimalFormat = new DecimalFormat("#.#####", new DecimalFormatSymbols(Locale.ENGLISH));

        // Creation of variables needed for the connection to the MQTT Broker
        username = getString(R.string.username);
        password = getString(R.string.password);
        deviceID = UUID.randomUUID().toString();
        mqttClient = new MQTTClient(this, getString(R.string.serverURI), deviceID);
        broadcastTopic = getString(R.string.broadcastTopic);
        mqttClientConnected = false;
        Log.d(getClass().toString(), "MQTT client created with ID " + deviceID);

        // Creation of Sensor and Location managers, listeners and handlers
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorListener = new ApplicationSensorListener();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new ApplicationLocationListener();

        accelerometerHandler = new AccelerometerHandler(sensorManager, sensorListener);
        orientationHandler = new OrientationHandler(sensorManager, sensorListener);
        locationHandler = new LocationHandler(locationManager, locationListener, this);

        // Reading the default values from configuration resource
        samplingPeriod = getResources().getInteger(R.integer.samplingPeriod);
        accelerometer = getResources().getBoolean(R.bool.accelerometer);
        orientation = getResources().getBoolean(R.bool.orientation);
        location = getResources().getBoolean(R.bool.location);

        // Setting textViews relative to configuration
        ((TextView) findViewById(R.id.samplingPeriodTextView)).setText(String.valueOf(samplingPeriod));
        findViewById(R.id.accelerometerTextView).setEnabled(accelerometer);
        findViewById(R.id.orientationTextView).setEnabled(orientation);
        findViewById(R.id.locationTextView).setEnabled(location);
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        if(mqttClientConnected)
            mqttClient.disconnect(new DisconnectionListener());

        super.onDestroy();
    }

    /**
     * This method provides an useful and simple interface to the Toast class,
     * in order to show custom toast messages on the display.
     * @param text
     */
    protected void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    /**
     * This method retrieves the current timestamp with a milliseconds format.
     * @return the current timestamp
     */
    public static String getTimestamp() {
        return new SimpleDateFormat("dd.MM.yy HH:mm:ss.SSS").format(new Date());
    }

    /**
     * This method is called when the user click the "Connect" button.
     * The MQTT Client will be connected to the broker, whose URI is written in the client object,
     * through username and password, which are written in the configuration file,
     * and it will be subscribed on the broadcast topic, whose nome is also written in this resource.
     *
     * On the broadcast topic, the coordinator will send the configuration for the scanning of values
     * and the device will receive it.
     *
     * @param view: the "Connect" button
     */
    public void onConnect(View view) {
        mqttClient.connect(username, password, new ConnectionListener(), new ConnectionCallback());
    }

    /**
     * This method is called when the user clicks the "Disconnect" button
     * and causes the stop of all the functionalities of the application.
     *
     * @param view
     */
    public void onDisconnect(View view) {
        mqttClient.disconnect(new DisconnectionListener());
    }

    /**
     * This method is called when the user clicks the "Start" button
     * and allows to start to read the sensors and send the data to the MQTT broker.
     *
     * @param view
     */
    public void onStart(View view) {
        if(accelerometer) {
            accelerometerSensor = accelerometerHandler.registerListener();
            if(accelerometerSensor == null)
                accelerometer = false;
        }

        if(orientation) {
            orientationSensor = orientationHandler.registerListener();
            if(orientationSensor == null)
                orientation = false;
        }

        if(location) {
            if (!locationHandler.registerListener()) {
                /*
                 Permission for location is going to be requested and
                 the onRequestPermissionsResult method will be called.
                 */
                Log.d(getClass().toString(), "Permission denied for location! Requesting...");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST_CODE);
            } else
                if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                    showToast("Enable GPS!");
        }

        Log.d(getClass().toString(), String.format("Accelerometer: %b\n" +
                "Orientation: %b\nLocation: %b", accelerometer, orientation, location));

        // Enabling the "Stop" button
        findViewById(R.id.stopButton).setEnabled(true);
        // Disabling the button itself
        view.setEnabled(false);
    }

    /**
     * This method is called when the user clicks the "Stop" button
     * and blocks to read the sensors and send the data to the MQTT broker.
     *
     * @param view
     */
    public void onStop(View view) {
        // Unregister the listeners for all sensors
        accelerometerHandler.unregisterListener();
        orientationHandler.unregisterListener();
        locationHandler.unregisterListener();

        // Enabling the "Start" button
        findViewById(R.id.startButton).setEnabled(true);
        // Disabling the button itself
        view.setEnabled(false);
    }

    /**
     * This class implements the Listener for the connection to the broker
     * and the methods for successful connection and failed connection.
     */
    protected class ConnectionListener implements IMqttActionListener {

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            String msg = "Connection successful!";

            Log.d(getClass().toString(), msg);
            showToast(msg);

            mqttClientConnected = true;
            // Enabling the buttons for disconnection and for starting to scan the values from sensors
            findViewById(R.id.disconnectButton).setEnabled(true);
            findViewById(R.id.startButton).setEnabled(true);

            // Disabling the itself button
            findViewById(R.id.connectButton).setEnabled(false);

            /*
             * The client subscribes at broadcastTopic with Quality of Service 1
             * because there are no problems if the client received the configuration more times.
             */
            mqttClient.subscribe(broadcastTopic + "/samplingPeriod", 1, new SubscriptionListener());
            mqttClient.subscribe(broadcastTopic + "/accelerometer", 1, new SubscriptionListener());
            mqttClient.subscribe(broadcastTopic + "/orientation", 1, new SubscriptionListener());
            mqttClient.subscribe(broadcastTopic + "/location", 1, new SubscriptionListener());

            /*
             * The client sends its ID to the coordinator, in order to be added into its collection of devices.
             * This collection will be used by the coordinator to broadcast messages to all devices.
             * The message is retained: so, the coordinator will see the device also if it connects after the device itself.
             */
            mqttClient.publish(broadcastTopic + "/newDevice", deviceID, 1, true, new PublicationListener());
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            String msg = "Connection failed!";
            Log.e(getClass().toString(), msg);
            showToast(msg);
        }
    }

    /**
     * This class implements the Callback for the connection to the broker
     * and, in particular, the method to manage the arrival of a message on the topics
     * which the client is subscribed at.
     */
    protected class ConnectionCallback implements MqttCallback {

        @Override
        public void connectionLost(Throwable cause) {
            Log.e(getClass().toString(), "Connection lost!");
            onStop(findViewById(R.id.stopButton));
            findViewById(R.id.disconnectButton).setEnabled(false);
            findViewById(R.id.startButton).setEnabled(false);

            // Enabling the "Connect" button
            findViewById(R.id.connectButton).setEnabled(true);
        }

        /**
         * This method manages the arrival of a message on the topics which the client is subscribed at.
         * The only topic which the client should be subscribed at is the broadcastTopic.
         * @param topic
         * @param message
         * @throws Exception
         */
        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.d(getClass().toString(), String.format("Message (%s) arrived from %s!", message.toString(), topic));

            // Checking which configuration message is arrived and setting the relative variable
            if(topic.equals(broadcastTopic + "/samplingPeriod")) {
                try {
                    samplingPeriod = Integer.parseInt(message.toString());
                } catch(NumberFormatException ex) {
                    Log.e(getClass().toString(), "The sampling period is not an integer! Setting default value...");
                    samplingPeriod = getResources().getInteger(R.integer.samplingPeriod);
                }
                ((TextView) findViewById(R.id.samplingPeriodTextView)).setText(String.valueOf(samplingPeriod));
            } // If the String value is neither "true" nor "false", "false" will be set.
            else if(topic.equals(broadcastTopic + "/accelerometer")) {
                accelerometer = Boolean.parseBoolean(message.toString());
                findViewById(R.id.accelerometerTextView).setEnabled(accelerometer);
            } else if(topic.equals(broadcastTopic + "/orientation")) {
                orientation = Boolean.parseBoolean(message.toString());
                findViewById(R.id.orientationTextView).setEnabled(orientation);
            } else if(topic.equals(broadcastTopic + "/location")) {
                location = Boolean.parseBoolean(message.toString());
                findViewById(R.id.locationTextView).setEnabled(location);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            //Log.d(getClass().toString(), "Delivery complete!");
        }
    }

    protected class DisconnectionListener implements IMqttActionListener {

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            String msg = "Disconnection successful!";

            Log.d(getClass().toString(), msg);
            showToast(msg);

            mqttClientConnected = false;
            /*
             * Disabling the buttons for disconnection and for starting to scan the values from sensors
             * and stops to read the sensors. This step is necessary in order to avoid waste of energy.
             */
            onStop(findViewById(R.id.stopButton));
            findViewById(R.id.disconnectButton).setEnabled(false);
            findViewById(R.id.startButton).setEnabled(false);

            // Enabling the "Connect" button
            findViewById(R.id.connectButton).setEnabled(true);
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            String msg = "Disconnection failed!";
            Log.e(getClass().toString(), msg);
            showToast(msg);
        }
    }

    /**
     * This class implements the Listener for the subscription to a topic
     * and the methods for successful connection and failed subscription.
     */
    protected class SubscriptionListener implements IMqttActionListener {

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Log.d(getClass().toString(), "Subscription successful!");
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            Log.e(getClass().toString(), "Subscription failed!");
        }
    }

    /**
     * This class implements the Listener for the publication of a message
     * and the methods for successful connection and failed publication.
     */
    protected class PublicationListener implements IMqttActionListener {

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            //Log.d(getClass().toString(), "Publication successful!");
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            Log.e(getClass().toString(), "Publication failed!");
        }
    }

    /**
     * This class implements the Listener for all the events relative to
     * the accelerometer and the orientation.
     */
    protected class ApplicationSensorListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) {
            Sensor sensor = event.sensor;
            String values = decimalFormat.format(event.values[0]) +
                    "," + decimalFormat.format(event.values[1]) +
                    "," + decimalFormat.format(event.values[2]);
            String msg = String.format("%s;%s", getTimestamp(), values);

            /*
             The Quality of Service is 1 because
             the coordinator will sample the data according to a certain period.
             */
            if(sensor.equals(accelerometerSensor)) {
                Log.d(getClass().toString(), "Accelerometer: " + msg);
                ((TextView) findViewById(R.id.accelerometerDataTextView)).setText(values);
                mqttClient.publish(deviceID + "/accelerometer", msg,
                        1, true, new PublicationListener());
            } else if(sensor.equals(orientationSensor)) {
                Log.d(getClass().toString(), "Orientation: " + msg);
                ((TextView) findViewById(R.id.orientationDataTextView)).setText(values);
                mqttClient.publish(deviceID + "/orientation", msg,
                        1, true, new PublicationListener());
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(getClass().toString(),
                    String.format("Accuracy of %s changed to %d!",
                            sensor.toString(), accuracy));
        }
    }

    /**
     * This class implements the Listener for all the events relative to
     * the location.
     */
    protected class ApplicationLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(@NonNull Location location) {
            String values = decimalFormat.format(location.getLatitude()) + "," +
                    decimalFormat.format(location.getLongitude());
            String msg = String.format("%s;%s", getTimestamp(), values);

            /*
             The Quality of Service is 2 because it is crucial that
             the coordinator receives the data exactly one time.
             */
            Log.d(getClass().toString(), "Location: " + msg);
            ((TextView) findViewById(R.id.locationDataTextView)).setText(values);
            mqttClient.publish(deviceID + "/location", msg,
                    2, false, new PublicationListener());
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            showToast("Enable GPS!");
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            Log.d(getClass().toString(), "GPS is now enabled");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    /**
     * This method is called when some permissions are requested,
     * through ActivityCompat.requestPermissions method.
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(getClass().toString(), "Permission just granted for location!");
                locationHandler.registerListener();
                if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                    showToast("Enable GPS!");
            } else {
                location = false;
                ((TextView) findViewById(R.id.locationDataTextView)).setText("Permission denied!");
                Log.e(getClass().toString(), "Permission just denied for location!");
            }
        } else {
            throw new IllegalStateException("Unexpected request code: " + requestCode);
        }
    }
}