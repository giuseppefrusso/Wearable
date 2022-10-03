package it.unisa.diem.wearable.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class OrientationHandler extends AbstractSensorHandler {

    public OrientationHandler(SensorManager sensorManager, SensorEventListener sensorEventListener) {
        super(sensorManager, sensorEventListener);
    }

    @Override
    public Sensor registerListener() {
        Sensor orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        if(orientationSensor != null)
            sensorManager.registerListener(sensorEventListener, orientationSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        return orientationSensor;
    }

    @Override
    public void unregisterListener() {
        sensorManager.unregisterListener(sensorEventListener);
    }
}
