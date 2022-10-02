package it.unisa.diem.wearable.communication;

import android.content.Context;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTClient {

    private MqttAndroidClient mqttClient;
    private Context context;

    public MQTTClient(Context context,
                      String serverURI,
                      String clientID) {
        mqttClient = new MqttAndroidClient(context, serverURI, clientID);
        this.context = context;
    }

    public void connect(String username,
                        String password,
                        IMqttActionListener cbConnect,
                        MqttCallback cbClient) {
        mqttClient.setCallback(cbClient);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        try {
            mqttClient.connect(options, this.context, cbConnect);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String topic,
                          int qos,
                          IMqttActionListener cbSubscribe) {
        try {
            mqttClient.subscribe(topic, qos, this.context, cbSubscribe);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void unsubscribe(String topic,
                          IMqttActionListener cbUnsubscribe) {
        try {
            mqttClient.unsubscribe(topic, this.context, cbUnsubscribe);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topic,
                        String msg,
                        int qos,
                        boolean retained,
                        IMqttActionListener cbPublish) {
        MqttMessage message = new MqttMessage();
        message.setPayload(msg.getBytes());
        message.setQos(qos);
        message.setRetained(retained);
        try {
            mqttClient.publish(topic, message, this.context, cbPublish);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void disconnect(IMqttActionListener cbDisconnect) {
        try {
            mqttClient.disconnect(this.context, cbDisconnect);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
