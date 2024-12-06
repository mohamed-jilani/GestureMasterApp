package jilani.group.gesturemasterapp;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class DriverList extends AppCompatActivity {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdate = 0;
    private static final int SHAKE_THRESHOLD = 800;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private ListView listView;
    private ActionAdapter adapter;
    private ArrayList<ActionItem> actions;
    private HashMap<Integer, Integer> frottementActions;
    private int frottementCount = 0;
    private Handler handler = new Handler();
    private Runnable frottementRunnable;

    private MediaRecorder mediaRecorder;
    private String outputFilePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frottementActions = new HashMap<>();
        actions = new ArrayList<>();

        actions.add(new ActionItem("Open Google Maps", 0, R.drawable.ic_maps));
        actions.add(new ActionItem("Send SMS", 0, R.drawable.ic_sms));
        actions.add(new ActionItem("Play Favorite Music", 0, R.drawable.ic_music));

        listView = findViewById(R.id.list_actions);
        adapter = new ActionAdapter(this, actions);
        listView.setAdapter(adapter);

        // Supprimez l'appel redondant à setOnItemClickListener
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 2) {
                // Si "Play Favorite Music" est cliqué, on redirige vers MusicIntoActivity
                if (position == 2) {
                    Intent musicIntent = new Intent(DriverList.this, MusicIntoActivity.class);
                    startActivity(musicIntent);
                }
            } else {
                // Sinon, afficher le dialogue pour configurer les frottements
                showEditDialog(position);
            }
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        checkPermissions();
        ImageView backIcon = findViewById(R.id.back_icon);
        backIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(DriverList.this, CardActivityActivity.class);
                startActivity(i);
            }
        });
    }

    private void checkPermissions() {
        // Liste des permissions nécessaires
        String[] permissions = {
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.CALL_PHONE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.ACCESS_NETWORK_STATE,
                android.Manifest.permission.CHANGE_WIFI_STATE,
                android.Manifest.permission.WRITE_SETTINGS,
                android.Manifest.permission.INTERNET,
                android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
                android.Manifest.permission.ACCESS_WIFI_STATE,
                android.Manifest.permission.MANAGE_OWN_CALLS,
                android.Manifest.permission.SYSTEM_ALERT_WINDOW,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
        };

        // Liste des permissions à demander
        ArrayList<String> permissionsToRequest = new ArrayList<>();

        // Vérification de chaque permission
        for (String permission : permissions) {
            // Si la permission n'est pas encore accordée
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        // Si des permissions doivent être demandées
        if (!permissionsToRequest.isEmpty()) {
            // Demander les permissions manquantes
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), 1);
        }
    }

    private void onFrottementDetected() {
        frottementCount++;

        if (frottementRunnable != null) {
            handler.removeCallbacks(frottementRunnable);
        }

        frottementRunnable = () -> {
            executeActionBasedOnFrottementCount();
            frottementCount = 0;
        };

        handler.postDelayed(frottementRunnable, 1000);
    }

    private void executeActionBasedOnFrottementCount() {
        if (frottementActions.containsKey(frottementCount)) {
            int actionPosition = frottementActions.get(frottementCount);
            performAction(actionPosition);
        } else {
            Toast.makeText(this, "Aucune action assignée pour " + frottementCount + " frottements", Toast.LENGTH_SHORT).show();
        }
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                long currentTime = System.currentTimeMillis();
                long diffTime = currentTime - lastUpdate;

                if (diffTime > 100) {
                    lastUpdate = currentTime;

                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    float acceleration = (x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH;

                    if (acceleration > SHAKE_THRESHOLD) {
                        onFrottementDetected();
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };
    private void performAction(int position) {
        switch (position) {
            case 0:
                openGoogleMaps();
                break;
            case 1:
                sendSMS();
                break;
            default:
                Toast.makeText(this, "Action non définie", Toast.LENGTH_SHORT).show();
        }
    }






    private boolean isPermissionRequested = false;

    private void openGoogleMaps() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // URI pour ouvrir Google Maps
            Uri uri = Uri.parse("geo:0,0?q=1600+Amphitheatre+Parkway,+Mountain+View,+California");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
            mapIntent.setPackage("com.google.android.apps.maps");

            // Vérifiez si l'application Google Maps est disponible
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // Si Google Maps n'est pas installé, redirigez vers un navigateur
                Toast.makeText(this, "Google Maps n'est pas installé. Ouverture dans le navigateur.", Toast.LENGTH_SHORT).show();
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.tn/maps/place/Institut+Sup%C3%A9rieur+des+Etudes+Technologiques+de+Rades/@36.7603043,10.2673499,17z/data=!3m1!4b1!4m6!3m5!1s0x12fd49fa15643927:0xad64c8c462b52435!8m2!3d36.7603!4d10.2699248!16s%2Fg%2F12156nc0?hl=fr&entry=ttu&g_ep=EgoyMDI0MTIwMy4wIKXMDSoASAFQAw%3D%3D"));
                startActivity(browserIntent);
            }
        } else {
            // Demande de permission si elle n'est pas accordée
            isPermissionRequested = true;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission accordée
                if (isPermissionRequested) {
                    // Re-essayer d'ouvrir Google Maps
                    openGoogleMaps();
                    isPermissionRequested = false; // Réinitialiser le marqueur
                }
            } else {
                // Permission refusée, afficher un message à l'utilisateur
                Toast.makeText(this, "La permission de localisation est requise pour utiliser cette fonctionnalité.", Toast.LENGTH_LONG).show();
            }
        }
    }

    //music




    //SMS
    private void sendSMS() {
        String phoneNumber = "53107137";
        String message = "Test de l'application SMS";

        // Créer un Intent pour ouvrir l'application de messagerie
        Intent smsIntent = new Intent(Intent.ACTION_VIEW);
        smsIntent.setData(Uri.parse("sms:" + phoneNumber)); // Numéro de téléphone prérempli
        smsIntent.putExtra("sms_body", message); // Message prérempli

        try {
            // Démarrer l'application de messagerie
            startActivity(smsIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Aucune application de messagerie trouvée", Toast.LENGTH_SHORT).show();
        }
    }






    //Dialogue
    private void showEditDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Configurer les frottements pour : " + actions.get(position).getTitle());

        String[] options = {"1 frottement", "2 frottements", "3 frottements", "4 frottements"};
        builder.setItems(options, (dialog, which) -> {
            int selectedFrottements = which + 1;
            if (frottementActions.containsKey(selectedFrottements)) {
                int existingPosition = frottementActions.get(selectedFrottements);
                actions.get(existingPosition).setFrottements(0);
            }
            frottementActions.put(selectedFrottements, position);
            actions.get(position).setFrottements(selectedFrottements);
            adapter.notifyDataSetChanged();
        });

        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }


}