package com.mas.iotignitehealth;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;


/*GoogleApiClient.ConnectionCallbacks arayüzünü etkinliğe uyguladıktan sonra,
onConnected() ve onConnectionSuspended() metotlarını eklemeliyiz.
GoogleApiClient.OnConnectionFailedListener arayüzünü etkinliğe uyguladıktan sonra,
onConnectionFailed() metodunu eklemeliyiz.*/
public class MainActivity extends WearableActivity implements SensorEventListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    /*Değişkenlerimiz*/
    String rate = "250";
    private TextView textViewHeart;
    String wearable_data_path;
    GoogleApiClient googleClient;
    SensorManager sensorManager;
    Sensor heartRateSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        textViewHeart = findViewById(R.id.heart);
        getHeartRate();

    }

    public void sendRate(View v) {
        createGoogleApiClient();
    }

    private void createGoogleApiClient() {
        /*Data Layer API’sine ulaşmak için GoogleApiClient sınıfını kullanırız.
        Bu sınıf ile Google Play servislerine ulaşabiliriz. Diğer tüm hizmetlerin ana
        giriş noktası bu sınıftır.
        Bu sınıf içerisinde çeşitli statik metotlar bulunmaktadır.
        Amacımız, sadece Wearable.API servisine erişmektir. Bu servisi kullanarak
        iki cihaz arasında veri senkronizasyonu işlemini yapacağız.
        Etinliğe eklediğimiz, onConnected(), onConnectionSuspended() ve
        onConnectionFailed() metotlarını bu istemci ile ilşkilendşrmek için
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        verilen metotları kesinlikle istemci ile ilişkilendirmeliyiz.
        bunları yazmadığımızda, eklediğimiz metotlar çalışmayacaktır.
        dolayısıyla herhangi bir veri senkronizasyonu da olmayacaktır.
        build(): Bu metot ile istemci oluşturulur. Tabi bu işlemler ile sadece istemciyi
        oluşturduk. istemciyle çalışabilmek için connect() metodunu kullanmalıyız.
        */
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        /*path bilgisi tanımlanır.*/
        wearable_data_path = "/wearable_data";

        /*connect() bu metot ile onConnected() metodu çağrılır.*/
        googleClient.connect();
    }

    private void getHeartRate() {
        /*Nabız ölçümü yapan sensör çalışmaya başlar.*/
        sensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }


    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onSensorChanged(SensorEvent event) {
        /*Nabız sensöründen gelen veriler saat ekranında gösterilir.*/
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            rate = "" + (int) event.values[0];
            textViewHeart.setText("Kalp Atış Hızı:" + rate);
        }
    }


    /*onConnected(): connect() metodundan sonra, bağlanma isteği
    başarıyla tamamlandığında bu yöntem asenkron olarak çağrılır.*/
    @Override
    public void onConnected(@Nullable Bundle bundle) {

        /*DataMap: Öncelikle DataMap yapısını oluşturmalıyız. Bu yapı
        içerisine, akıllı saate göndermek istediğimiz değerleri eklemeliyiz.*/
        DataMap dataMap = new DataMap();

        /*DataMap sınıfı yapı olarak Bundle sınıfında benzer. Veriler
        key-value olarak tutulur. key ifadesinin bu veriye erişmek için
        kullanacağız.*/
        dataMap.putString("rate", rate);

        /*DataMap oluşturduktan sonra PutDataMapRequest nesnesi
        oluşturmalıyız. Bu nesnenin temel amacı, iletilecek verinin path
        yani yol bilgisini belirtmektir. Bunun için create() metodu
        kullanılır. Path bilgisini akıllı saatte bu veriye erişmek için
        kullanacağız.
        setUrgent(): Eğer yapılacak işlem çok önemli ise setUrgent()
        metodu ile işlemin acil olduğunu belirtmelisiniz. Örneğin, akıllı
        saatten, akıllı telefondaki müzik uygulaması kontrol edildiğinde
        işlemin gecikmeden yapılması beklenir. Bu durumda yukarıdaki
        metodun kullanımı çok önemlidir.
        setUrgent() metodu çağırılmadığı zaman, sistem yapılan
        istekleri 30 dakika kadar geciktirebilir. Bu maksimum değerdir.
        Genellikle birkaç dakika içinde işlem yapılır. */
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(wearable_data_path).setUrgent();

        /*DataMap ile oluşturulan verilerin, PutDataMapRequest nesnesine
        eklenmesi için getDataMap() ve putAll() metotlarını kullanırız.
        İletilecek veriler aşağıdaki gibi nesneye eklenir.*/
        putDataMapRequest.getDataMap().putAll(dataMap);

        /*PutDataMapRequest içinde bulunan verilere sahip olan
        PutDataRequest nesnesi oluşturulur. */
        PutDataRequest request = putDataMapRequest.asPutDataRequest();

        /*request yani istek ağa gönderilir. Eğer akıllı saat ve telefon bağlı
        değilse, bağlantı yeniden kurulduğunda veriler belleğe alınır ve
        senkronize edilir. Yani cihazın o an bağlı olma şartı yoktur.
        Bağlantı kurulduğunda veri otomatik olarak akıllı saate iletilir.*/
        Wearable.DataApi.putDataItem(googleClient, request);

    }


    /*onConnectionSuspended(): İstemci bağlantısı geçici olarak kesildiği
    durumlarda bu metot çağrılır*/
    @Override
    public void onConnectionSuspended(int i) {

    }

    /*onConnectionFailed(): Bağlantının başarısız olduğu olayları
    dinlemek için bir dinleyici kayıt eder. */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        /*Kullanıcı etkinlikte ayrıldığında, eğer istemci bulunuyorsa ve bu istemci aktif ise
        istemci sonlandırılır.*/
        if (googleClient != null && googleClient.isConnected()) {
            googleClient.disconnect();

        }
        sensorManager.unregisterListener(this, heartRateSensor);
    }


}
