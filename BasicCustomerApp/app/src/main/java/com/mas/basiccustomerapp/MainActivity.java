package com.mas.basiccustomerapp;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;


public class MainActivity extends AppCompatActivity {

    /*Arayüzde bulunan kontroller için değişken tanımlama.*/
    private TextView temperatureValue;
    private SeekBar seekBarTemperature;
    private ImageView imageLamp;
    private ToggleButton toggleLamp;
    private String centigrade = " \u00b0 C";

    /*VirtualCustomerNodeHandler bu sınıf özetle
    IOTIGNITE PLATFORMUNA BAĞLANTI,
    THING OLUŞTURMA VE IGNITE PLATFORMUNA KAYIT ETMEK,
    MAINACTIVITY ARAYÜZÜNDE BULUNAN SENSÖR VERİLERİNİ OKUMAK VEYA OKUMAYI İPTAL ETMEK,
    SENSÖR VERİLERİNİ IGNITE ORTAMINA GÖNDERMEK ve daha birçok
    işlemi yapmak için kullanılmaktadır.*/
    private VirtualCustomerNodeHandler customerNodeHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*Parametre olarak MainActivity activitysini gönderdik.*/
        customerNodeHandler = new VirtualCustomerNodeHandler(this);

        /*start metodu ile IoT-Ignite platformuna bağlantı sağlanır.*/
        customerNodeHandler.start();

        /*Arayüz için bu metot çağrılır.*/
        initUIComponents();
    }

    /*MainActivity arayüzünde bulunan kontrollere erişim sağlanır.*/
    private void initUIComponents() {

        /*Sıcaklık sensörü için kontrol erişimi*/
        seekBarTemperature = findViewById(R.id.seekBarTemperature);
        temperatureValue = findViewById(R.id.textTemperatureValue);

        /*Kontrollere ilk değerler atanır. Sıcaklık 21, lamba ise yanık olur.*/
        seekBarTemperature.setProgress(Constants.FIRST_VALUE_FOR_TEMP);
        temperatureValue.setText(String.format("%d" + centigrade, Constants.FIRST_VALUE_FOR_TEMP));

        /*sıcaklık 0 ile 100 arasında üretilir.*/
        seekBarTemperature.setMax(100);

        /*SeekBar değeri değiştiğinde algılamayı sağlayan bir listener eklenir.*/
        seekBarTemperature.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                /*Yeni değer kullanıcının okuyabilmesi için textview kontrolüne yazılır.*/
                temperatureValue.setText(String.format("%d" + centigrade,i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                /*SeekBar üzerinde yapılan hareket durduğunda üretilen değer
                sendData metodu ile Ignite platformuna gönderilir.*/
                customerNodeHandler.sendData(Constants.TEMP_NAME, seekBar.getProgress());
            }
        });

        /*Lamba için kontrol erişimi*/
        imageLamp = findViewById(R.id.imageLamp);
        toggleLamp = findViewById(R.id.toggleLamp);

        /*Lamabımız başlangıçta açık olarak ayarlanır.*/
        toggleLamp.setChecked(true);

        /*Açık lamba resmi kullanıcıya gösterilir.*/
        imageLamp.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.lamp_on));

        /*Toggle'ın işaret durumu değiştiğinde algılamayı sağlayan bir listener tanımlanır.*/
        toggleLamp.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                /*Toggle'ın işaret durumu yani true veya false durumu isChecked değişkenindendir.
                İşarete göre arayüzdeki görsel değiştirilir. Her iki sonuç içinde üretilen veri Ignite
                ortamına sendData ile gönderilir.*/
                if(isChecked) {
                    imageLamp.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.lamp_on));
                    customerNodeHandler.sendData(Constants.LAMP_NAME, 1);
                } else {
                    imageLamp.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.lamp_off));
                    customerNodeHandler.sendData(Constants.LAMP_NAME, 0);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*Uygulama sonlandığı zaman Ignite bağlantısı kesilir.*/
        customerNodeHandler.stop();
    }
}
