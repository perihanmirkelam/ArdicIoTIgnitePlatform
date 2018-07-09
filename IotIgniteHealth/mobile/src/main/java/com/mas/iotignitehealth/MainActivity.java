package com.mas.iotignitehealth;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    /*Arayüzde bulunan kontroller için değişken tanımlama.*/
    private TextView heartRateSensorValue;
    private SeekBar heartRateSensor;

    /*VirtualHumanNodeHandler bu sınıf özetle
    IOTIGNITE PLATFORMUNA BAĞLANTI,
    THING OLUŞTURMA VE IGNITE PLATFORMUNA KAYIT ETMEK,
    MAINACTIVITY ARAYÜZÜNDE BULUNAN SENSÖR VERİLERİNİ OKUMAK VEYA OKUMAYI İPTAL ETMEK,
    SENSÖR VERİLERİNİ IGNITE ORTAMINA GÖNDERMEK ve daha birçok
    işlemi yapmak için kullanılmaktadır.*/
    private VirtualHumanNodeHandler humanNodeHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*Parametre olarak MainActivity activitysini gönderdik.*/
        humanNodeHandler = new VirtualHumanNodeHandler(this);

        /*start metodu ile IoT-Ignite platformuna bağlantı sağlanır.*/
        humanNodeHandler.start();
        getControlView();

        /*getBundleExtra(): Bundle olarak gönderilen veri alınır. Bunun için serviste
        tanımladığımız key bilgisini kullanırız.*/
        Bundle data = getIntent().getBundleExtra("data");
        if (data != null) {
            /*Gelen metin değişkene atanır. Metne erişmek için Akıllı saat için
            yazdığımız DataSendActivity isimli activityde tanımladığımız key bilgisini
            kullanırız.*/
            heartRateSensor.setProgress(Integer.parseInt(data.getString("rate")));
            heartRateSensorValue.setText(data.getString("rate"));

        }
        /*Arayüz için bu metot çağrılır.*/
        initUIComponents();
    }

    public void getControlView() {
        /*Heart Rate sensörü için kontrol erişimi*/
        heartRateSensor = findViewById(R.id.heartRateSensor);
        heartRateSensorValue = findViewById(R.id.heartRateSensorValue);

        /*Kontrollere ilk değerler atanır. Heart Rate 80 olur*/
        heartRateSensor.setProgress(Constants.FIRST_VALUE_FOR_HEART_RATE);
        heartRateSensorValue.setText(String.format(Locale.getDefault(), "%d", Constants.FIRST_VALUE_FOR_HEART_RATE));

        /*rate 0 ile 250 arasında üretilir.*/
        heartRateSensor.setMax(250);
    }

    /*MainActivity arayüzünde bulunan kontrollere erişim sağlanır.*/
    private void initUIComponents() {

        /*Heart Rate değeri değiştiğinde algılamayı sağlayan bir listener eklenir.*/
        heartRateSensor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                /*Yeni değer, kullanıcının okuyabilmesi için textview kontrolüe yazılır.*/
                heartRateSensorValue.setText(String.format(Locale.getDefault(), "%d", i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                /*SeekBar üzerinde yapılan hareket durduğunda üretilen değer
                sendData metodu ile Ignite platformuna gönderilir.*/
                humanNodeHandler.sendData(Constants.HEART_RATE_SENSOR, seekBar.getProgress());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*Uygulama sonlandığı zaman Ignite bağlantısı kesilir.*/
        humanNodeHandler.stop();
    }

}
