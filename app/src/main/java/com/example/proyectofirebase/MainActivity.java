package com.example.proyectofirebase;

import android.os.Bundle;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.view.View;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import android.widget.Button;
import android.widget.TextView;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity {

    //MQTT
    private static String mqttHost = "tcp://mqttfirebase.cloud.shiftr.io:1883";
    private static String IdUsuario = "AppAndroid";
    private static String Topico = "Mensaje";

    private static String User = "mqttfirebase";
    private static String Pass = "60qK21vVNuFbytHt";

    private TextView textView;
    private EditText editTextMessage;
    private Button botonEnvio;

    private MqttClient mqttClient;

    //FIN MQTT

    private EditText txtModelo,txtNombre,txtDueño, txtDireccion;
    private ListView lista;
    private Spinner spMarca;

    private FirebaseFirestore db;

    String[] TiposMarca = {"Samsung","Apple","Huawei"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //MQTT
        textView = findViewById(R.id.textView);
        editTextMessage = findViewById(R.id.txtMensaje);
        botonEnvio = findViewById(R.id.botonEnvioMensaje);

        try {
            mqttClient = new MqttClient(mqttHost, IdUsuario, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(User);
            options.setPassword(Pass.toCharArray());

            mqttClient.connect(options);
            Toast.makeText(this,"Aplicacion conectada al Sevidor MQTT", Toast.LENGTH_LONG).show();

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d("MQTT", "Conexion perdida");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    runOnUiThread(() -> textView.setText(payload) );

                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTT", "Entrega Completa");
                }
            });


        }catch (MqttException e){
            e.printStackTrace();
        }

        botonEnvio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mensaje = editTextMessage.getText().toString();
                try {
                    if (mqttClient != null && mqttClient.isConnected()) {
                        mqttClient.publish(Topico, mensaje.getBytes(), 0, false);
                        textView.append("\n -"+ mensaje);
                        Toast.makeText(MainActivity.this, "Mensaje Enviado", Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(MainActivity.this, "Error: no se pudo enviar el mensaje. La conexion MQTT no esta activa.", Toast.LENGTH_SHORT).show();
                    }
                }catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });


        //FIN MQTT

        CargarListaFirestore();

        db = FirebaseFirestore.getInstance();

        txtModelo = findViewById(R.id.txtModelo);
        txtNombre = findViewById(R.id.txtNombre);
        txtDueño = findViewById(R.id.txtDueño);
        txtDireccion =findViewById(R.id.txtDireccion);
        spMarca = findViewById(R.id.spMarca);
        lista = findViewById(R.id.lista);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, TiposMarca);

    }

    public void enviarDatosFirestore(){
        String modelo = txtModelo.getText().toString();
        String nombre = txtNombre.getText().toString();
        String dueño = txtDueño.getText().toString();
        String direccion = txtDireccion.getText().toString();
        String tipoMarca = spMarca.getSelectedItem().toString();

        Map<String, Object> celular = new HashMap<>();
        celular.put("modelo", modelo);
        celular.put("nombre", nombre);
        celular.put("dueño", dueño);
        celular.put("direccion", direccion);
        celular.put("tipoMarca", tipoMarca);


        db.collection("celulares")
                .document(modelo)
                .set(celular)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Datos enviados a Firestore correctamente", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error al enviar datos a Firestore:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public void CargarLista(View view) {
        CargarListaFirestore();
    }


    public void CargarListaFirestore(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("celulares")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<String> listaCelulares = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()){
                                String linea = "||" + document.getString("modelo") + "||" +
                                        document.getString("nombre") + "||" +
                                        document.getString("dueño") + "||" +
                                        document.getString("direccion");
                                listaCelulares.add(linea);
                            }

                            ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                                    MainActivity.this,
                                    android.R.layout.simple_list_item_1,
                                    listaCelulares
                            );
                            lista.setAdapter(adaptador);
                        }else {
                            Log.e("TAG", "Error al obtener datos de Firestore", task.getException());
                        }
                    }
                });
    }

}