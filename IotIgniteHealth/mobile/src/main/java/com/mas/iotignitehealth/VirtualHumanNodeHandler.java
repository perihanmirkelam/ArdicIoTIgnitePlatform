package com.mas.iotignitehealth;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ardic.android.iotignite.callbacks.ConnectionCallback;
import com.ardic.android.iotignite.enumerations.NodeType;
import com.ardic.android.iotignite.enumerations.ThingCategory;
import com.ardic.android.iotignite.enumerations.ThingDataType;
import com.ardic.android.iotignite.exceptions.AuthenticationException;
import com.ardic.android.iotignite.exceptions.UnsupportedVersionException;
import com.ardic.android.iotignite.listeners.NodeListener;
import com.ardic.android.iotignite.listeners.ThingListener;
import com.ardic.android.iotignite.nodes.IotIgniteManager;
import com.ardic.android.iotignite.nodes.Node;
import com.ardic.android.iotignite.things.Thing;
import com.ardic.android.iotignite.things.ThingActionData;
import com.ardic.android.iotignite.things.ThingConfiguration;
import com.ardic.android.iotignite.things.ThingData;
import com.ardic.android.iotignite.things.ThingType;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/*VirtualHumanNodeHandler bu sınıf özetle
IOTIGNITE PLATFORMUNA BAĞLANTI,
THING OLUŞTURMA VE IGNITE PLATFORMUNA KAYIT ETMEK,
MAINACTIVITY ARAYÜZÜNDE BULUNAN SENSÖR VERİLERİNİ OKUMAK VEYA OKUMAYI İPTAL ETMEK,
SENSÖR VERİLERİNİ IGNITE ORTAMINA GÖNDERMEK ve daha birçok
işlemi yapmak için kullanılmaktadır.*/

/*onConnected() ve onDisconnected() metotlarını eklemek için
sınıfa ConnectionCallback arayüzü uygulanmalıdır.*/
public class VirtualHumanNodeHandler implements ConnectionCallback {

    /********DEĞİŞKENLER********/

    /*Uygulama içinde kullanacağımız değişkenlerimiz*/
    private static final String TAG = "Health App";

    /*ScheduledExecutorService: Komutları belirli bir gecikmeden sonra çalıştırmayı veya periyodik olarak yürütmeyi sağlar.*/
    private static volatile ScheduledExecutorService mExecutor;

    /*İş parçacığı havuzunda tutulacak iş parçacığı sayısı 2 tane olacaktır.*/
    private static final int NUMBER_OF_THREADS_IN_EXECUTOR = 2;

    /*EXECUTOR_START_DELAY: iş parçası için gecikme süresi tanımladık*/
    private static final long EXECUTOR_START_DELAY = 100L;

    /*Thing bileşenlerden veri okumayaı sağlayan bir görev tanımladık.
    Hashtable: Bu sınıf, key-value çiftlerinden oluşan bir karma tablo oluşturmayı sağlar.*/
    private Hashtable<String, ScheduledFuture<?>> tasks = new Hashtable<>();

    /*IotIgniteManager: IoT-Ignite pltaformuna bağlanmayı sağlayan temel sınıftır.*/
    private static IotIgniteManager mIotIgniteManager;

    /*Sanal node oluşturmak için Node sınıfı kullanılır.*/
    private Node mNode;

    /*Sanal Thing oluşturmak için Thing sınıfı kullanılır.*/
    private Thing mRateThing;

    /*ThingType: Sensörün türünü belirtir. Sensör tipi ve üretici hakkında bilgi depolamayı sağlar.*/
    private ThingType mRateThingType;

    /*ThingDataHandler: Thing verilerini Ignite ortamına göndermeyi sağlayan iş parçacığı.*/
    private ThingDataHandler mThingDataHandler;

    /*IoT-Ignite ortamına bağlantı durumunu tutan bir değişken tanımladık*/
    private boolean igniteConnected = false;

    /*Bağlantı işlemini kontrol etmek için oluşturulan görevin bekleme süresi*/
    private static final long IGNITE_TIMER_PERIOD = 5000L;

    /*Timer: Bir iş parçacığını gelecekte yürütmek için kullanılır.*/
    private Timer igniteTimer = new Timer();

    /*IgniteWatchDog İş parçacığı ile Ignite bağlantısı kontrol edilir.*/
    private IgniteWatchDog igniteWatchDog = new IgniteWatchDog();

    private Context ctx;
    private Activity activity;

    /********KURUCU METODUMUZ********/
    public VirtualHumanNodeHandler(Activity activity) {
        this.ctx = activity.getApplicationContext();
        this.activity = activity;
    }


    /********IOTIGNITE PLATFORMUNA BAĞLANTI ********/
    /*IgniteWatchDog İş parçacığı ile Ignite bağlantısı kontrol edilir.
    Bunun için sınıfın TimerTask sınıfından türetilmesi gerekiyor.
    TimerTask sınıfı bir defalık veya tekrarlanan görevlerin yürütülmesi sağlanır.*/
    private class IgniteWatchDog extends TimerTask {
        @Override
        public void run() {
            /*IoT-Ignite platformuna bağlantı yoksa bağlantı işlemi yapılmaya çalışılır.*/
            if (!igniteConnected) {
                Log.i(TAG, "Ignite bağlantısı kuruluyor...");
                /*start metodu ile bağlantı sağlanır.*/
                start();
            }
        }
    }

    /*Ignite platformuna bağlanmayı sağlayan metodumuz.*/
    public void start() {

        try {
            /*IotIgniteManager.Builder: IgniteManger oluşturulur.
            Bu sınıf IoT-Ignite platformuna bağlanmayı sağlar.
            setContext(): Bağlantı işleminin hangi uygulama için yapılacağı belirtilir.
            Buraya parametre olarak Context verilir.
            setConnectionListener: Parametre olarak ConnectionCallback arayüzü alır.
            Bağlantı sağlandığı zaman bu arayüz ile gelen onConnected()  metodu çağrılır.
            Bu arayüzü VirtualHumanNodeHandler sınıfına yukarıda uygulamıştık.
            build(): bu metot ile bağlantı işlemi sağlanır.*/
            mIotIgniteManager = new IotIgniteManager.Builder()
                    .setContext(ctx)
                    .setConnectionListener(this)
                    .build();

        } catch (UnsupportedVersionException e) {
            Log.e(TAG, e.toString());

        }
        /*Bağlantı sağlandıktan sonra timer durdurulur.*/
        cancelAndScheduleIgniteTimer();
    }

    /*cancelAndScheduleIgniteTimer: Ignite platformuna bağlantı sağlandıktan sonra Timer sonlandırılır.*/
    private void cancelAndScheduleIgniteTimer() {
        /*cancel: Planlanmış görevleri iptal ederek Timer nesnesini sonlandırır.*/
        igniteTimer.cancel();
        igniteWatchDog.cancel();

        /*Timer nesneleri yeniden oluşturulur.*/
        igniteWatchDog = new IgniteWatchDog();
        igniteTimer = new Timer();

        /*schedule(): igniteWatchDog iş parçası 500 milisaniye sonra tekrar başlatılır.*/
        igniteTimer.schedule(igniteWatchDog, IGNITE_TIMER_PERIOD);
    }

    public void stop() {
        if (igniteConnected) {
            /*Ignite ile bağlantı kesildiği zaman Node ve Thing bileşenleri
            çevrimdışı yapmak için setConnected() metodunu kullanırız.
            Bu metot bağlantı durumunu ayarlamayı sağlar. Metodun ikinci
            parametresine istediğiniz değeri verebilirsiniz.*/
            mRateThing.setConnected(false, "Application Destroyed");
            mNode.setConnected(false, "ApplicationDestroyed");
        }
        /*mExecutor null ise sonlandırılır.*/
        if (mExecutor != null) {
            mExecutor.shutdown();
        }
    }

    /*onConnected() ve onDisconnected() metotları ConnectionCallback arayüzü ile
    gelen metotlardır.
    Ignite platformuna bağlantı sağlanırsa onConnected() metodu çağrılır.*/
    @Override
    public void onConnected() {

        Log.i(TAG, "Ignite Bağlantısı Kuruldu!");
        igniteConnected = true;

        /*Bağlantı durumu aşağıdaki metot ile kullanıcı arayüzüne uygulanır.*/
        updateConnectionStatus(true);

        /*Ignite platformuna bağlantı sağlanırsa aşağıdaki metotlar icra edilir.*/
        /*Node ve Thing oluşturulup ignite platformuna kayıt edilir. Ayrıca Thing
        bileşenlere ilk değerleri atanır.*/
        initIgniteVariables();
        sendInitialData();
        cancelAndScheduleIgniteTimer();
    }

    /*Ignite platformuna bağlantı kurulamaz ise onDisconnected() metodu çağrılır.*/
    @Override
    public void onDisconnected() {
        Log.i(TAG, "Ignite Bağlantısı Kesildi!");
        igniteConnected = false;
        /*Bağlantı durumu aşağıdaki metot ile kullanıcı arayüzüne uygulanır.*/
        updateConnectionStatus(false);
        cancelAndScheduleIgniteTimer();
    }

    /*Ignite platformu ile bağlantının olup olmadığını
    MainActivity'de göstermek için bu metot kullanılır.
    Amaç kullanıcıya bağlantı durumu hakkında bilgi vermektir.*/
    private void updateConnectionStatus(final boolean connected) {
        if (activity != null) {
            /*runOnUiThread(): Main Thread üzerinde değişklik yapmak için bir Runnable
            tanımlar. Runnable ile arayüz güncellenir.*/
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    /*MainActivity arayüzünde bulunan iki kontrole erişim sağlanır.*/
                    ImageView imageViewConnection = activity.findViewById(R.id.imageViewConnection);
                    TextView textViewConnection = activity.findViewById(R.id.textConnection);

                    /*connected true ise kullanıcı arayüzünde bağlantının kurulduğu
                    false ise bağlantının kurulamadığı bilgisi verilir.*/
                    if (connected) {
                        imageViewConnection.setImageDrawable(activity.getResources().getDrawable(R.drawable.connected));
                        textViewConnection.setText("Bağlandı");
                    } else {
                        imageViewConnection.setImageDrawable(activity.getResources().getDrawable(R.drawable.disconnected));
                        textViewConnection.setText("Bağlanamadı");
                    }
                }
            });
        }
    }

    /********THING OLUŞTURMA VE IGNITE PLATFORMUNA KAYIT ETMEK********/
    /*initIgniteVariables(): Bu metot ile Node ve Thing oluşturulup IoT-Ignite platformuna kayıt edilir.*/
    private void initIgniteVariables() {
        /*ThingType: Sensörün türünü belirtir. Sensör tipi ve üretici hakkında bilgi depolamayı sağlar.
        Bu sınıfın kurucu metodu için üç parametre alır:
        THING_TYPE: Thing tipi bilgisi tanımlar.
        VENDOR: Thing hakkında üretici bilgisi tanımlar.
        THING_DATA_TYPE: Thing için veri tipi tanımlar.*/
        mRateThingType = new ThingType(Constants.HEART_RATE_SENSOR, Constants.VENDOR_INFO, ThingDataType.FLOAT);

        /*NodeFactory(): Node nesnesi oluşturulur.
        createNode() Bu metot oluşturulacak Node'un bilgilerini tanımlamayı sağlar.
        Parametre olarak şu bilgileri kullanır.
        NODE_ID: Node için benzersiz bir ID bilgisi.
        NODE_ID: Node için label etiket tanımlar. Benzersiz olmak zorunda değil.
        NODE_TYPE: Desteklenen Node türlerini tanımlar. GENERIC, RASPBERRY_PI ve ARDUINO_YUN parametrelerini alabilir.
        Burada genel amaçlı sanal bir sensör tanımladığımız için NodeType.GENERIC parametresini kullandık.
        4.Parametreye bu gibi durumlarda null verilir.
        NodeListener: Kayıt işlemini dinlemek için tanımlanır.*/
        mNode = IotIgniteManager.NodeFactory.createNode(Constants.NODE_ID,
                Constants.NODE_ID,
                NodeType.GENERIC,
                null,
                new NodeListener() {
                    @Override
                    public void onNodeUnregistered(String s) {
                        Log.i(TAG, Constants.NODE_ID + " kayıt edilmedi!!");
                    }
                });

        /*Node kayıtlı değilse kayıt edilir ve bağlantı sağlanır.*/
        if (!mNode.isRegistered() && mNode.register()) {
            mNode.setConnected(true, Constants.NODE_ID + " online");
            Log.i(TAG, mNode.getNodeID() + " kayıt edildi!");
        } else {
            mNode.setConnected(true, Constants.NODE_ID + " online");
            Log.i(TAG, mNode.getNodeID() + " zaten kayıtlı!");
        }

        /*Node daha önce kayıtlı ise bu durumda bu Node'a bağlı olan Thing yani sensör ve aktüatör oluşturabiliriz.*/
        if (mNode.isRegistered()) {

            /*createThing(): Thing oluşturmak için kullanılır. Şu parametreleri alır.
            THING_ID: Thing için ID bilgisi. Bunun eşsiz olması zorunludur.
            Thing Type: Thing tipini tanımlar. EXTERNAL, BUILTIN veya UNDEFINED değerlerini alır.
            Burada EXTERNAL kullandık. Çünkü sensörlerimiz Gateway dışında tanımlıdır.
            FLAG_STATE: Thing'in bir actuator gibi hareket edip etmeyeceğini tanımlar.
            mRateThing için bu değer false. Çünkü bunu aktüatör değil ritim sensörü olarak kullanacağız.
            Thing Listener: Olay dinleyici tanımlamayı sağlar. Bunun için rateThingListener olay dinleyicisini
            inceleyiniz.
            Son parametre null olacaktır.*/
            mRateThing = mNode.createThing(Constants.HEART_RATE_SENSOR,
                    mRateThingType,
                    ThingCategory.EXTERNAL,
                    false,
                    rateThingListener,
                    null);

            /*registerThingIfNotRegistered(): Bu metot ile sensörlerin Ignite
            platformuna kayıt edilmesi sağlanır.*/
            registerThingIfNotRegistered(mRateThing);
        }
    }

    /*Tanımladığımız ritim sensörünü dinlemek için ThingListener tanımladık.
    Bu olay dinleyicide 3 metot bulunmaktadır. Konfigürasyon ve action message verileri
    buradan alınır.*/
    private ThingListener rateThingListener = new ThingListener() {

        /*onConfigurationReceived(): Yapılandırma IoT-Ignite tarafından ayarlandığında bu metot icra edilir.*/
        @Override
        public void onConfigurationReceived(Thing thing) {
            Log.i(TAG, "Konfigürasyon alındı " + thing.getThingID());
            /*applyConfiguration: Thing bileşenlerden veri okumayı sağlayan metot */
            applyConfiguration(thing);
        }

        /*onActionReceived(): Thing bir aktüatör olarak ayarlanmışsa, gönderilen eylem mesajları burada ele alınır.
        Ritim sensörü bir aktüatör olmadığından burada bir işlem yapılmayacak*/
        @Override
        public void onActionReceived(String nodeId, String sensorId, ThingActionData thingActionData) {

        }

        /*onThingUnregistered(): Thing, Ignite ortamına kayıt edilmediğinde bu metot çağrılır.*/
        @Override
        public void onThingUnregistered(String nodeId, String sensorId) {
            Log.i(TAG, "Ritim sensörü kayıt edilmedi!");
            /*Thing kayıt edilemediği zaman veri okumayı sağlayan taskın durdurulması gerekir.*/
            stopReadDataTask(nodeId, sensorId);
        }
    };

    /*registerThingIfNotRegistered(): Kod içinde tanımlanan Thing nesnelerini
    ignite ortamına kayıt etmeyi sağlar.*/
    private void registerThingIfNotRegistered(Thing t) {
        /*Thing bileşenlerinin kayıtlı olup olmadığı kontrol edilip,
        kayıtlı olmayanların kayıt edilmesi sağlanır.*/
        if (!t.isRegistered() && t.register()) {
            /*Ignite ile bağlantı kesildiği zaman Node ve Thing bileşenleri
            çevrimdışı yapmak için setConnected() metodunu kullanırız.
            Bu metot bağlantı durumunu ayarlamayı sağlar. Metodunu ikinci
            parametresine istediğiniz değeri verebilirsiniz.*/
            t.setConnected(true, t.getThingID() + " bağlandı");
            Log.i(TAG, t.getThingID() + " kayıt edildi!");
        } else {
            t.setConnected(true, t.getThingID() + " bağlandı");
            Log.i(TAG, t.getThingID() + " zaten kayıtlı!");
        }
        /*applyConfiguration: Thing bileşenlerden veri okumayı sağlayan metot */
        applyConfiguration(t);
    }

    /********MAINACTIVITY ARAYÜZÜNDE BULUNAN SENSÖR VERİLERİNİ OKUMAK VEYA OKUMAYI İPTAL ETMEK********/
    /*applyConfiguration: Thing bileşenlerden veri okumayı sağlayan metot*/
    private void applyConfiguration(Thing thing) {

        if (thing != null) {

            /*stopReadDataTask(): Konfigürasyon alındığı zaman öncelikle veri okumayı
            sağlayan görevin durdurulması gerekir.*/
            stopReadDataTask(thing.getNodeID(), thing.getThingID());

            /*getThingConfiguration(): Thing için gelen konfigürasyonu almayı sağlar.
            getDataReadingFrequency(): Veri okuma sıklığını milisaniye cinsinden alır. Varsayılan
            olarak READING_DO_NOT_READ değerine sahiptir. Hiçbir yapılandırma yoksa bulut için veri gönderilmez.
            Eğer bu değer 0 ise, verinin geldiği anlaşılır.*/
            if (thing.getThingConfiguration().getDataReadingFrequency() > 0) {

                /*ThingDataHandler: Thing verilerini Ignite ortamına göndermeyi sağlayan iş parçacığı.*/
                mThingDataHandler = new ThingDataHandler(thing);

                /*Executors: ScheduledExecutorService nesnesi oluşturmayı sağlar.
                newScheduledThreadPool: Belirli bir gecikme sonrasında komut çalıştırmak veya düzenli olarak
                yürütmek için komutları zamanlayabilen bir iş parçacığı havuzu oluşturur.*/
                mExecutor = Executors.newScheduledThreadPool(NUMBER_OF_THREADS_IN_EXECUTOR);

                /*ScheduledFuture: ScheduledExecutorService için planlı bir eylem tanımlar.
                scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit):
                Verilen ilk gecikmeden sonra (EXECUTOR_START_DELAY), verilen periyotta (thing.getThingConfiguration().getDataReadingFrequency())
                bir eylem oluşturmayı sağlar. Aynı zamanda bu eylemi yürütmeyi de sağlar.
                Sonuç olarak Thing verisi Ignite ortamına gönderilir.*/
                ScheduledFuture<?> sf = mExecutor.scheduleAtFixedRate(mThingDataHandler, EXECUTOR_START_DELAY, thing.getThingConfiguration().getDataReadingFrequency(), TimeUnit.MILLISECONDS);

                /*thing ve node için ID bilgileri alınıp tek bir key elde edilir.*/
                String key = thing.getNodeID() + "|" + thing.getThingID();

                /*put: Hashtable'da yani task içine key-value eşleştirmesi ekler.*/
                tasks.put(key, sf);
            }
        }
    }

    /*MainActivity'den gelen verilerin alınması sağlanır.
    Daha sonra bu verilerin Ignite ortamına aktarılması sağlanır.
    Bunu Runnable sınıfından türettik.*/
    private class ThingDataHandler implements Runnable {

        /*Gelen Thing bileşeni tutacak nesnemiz*/
        Thing mThing;

        /*Kurucu metodumuz Thign bileşeni alır.*/
        ThingDataHandler(Thing thing) {
            mThing = thing;
        }

        @Override
        public void run() {

            /*ThingData: IoTIgnite ortamına Thing verilerini göndermek için kullanılır.
            Her Thing nesnesi veri göndermek veya aktüatörler gibi eylem mesajı almak
            için bir ThingData nesnesine ihtiyaç duyar.*/
            ThingData mThingData = new ThingData();

            /*Gelen Thing, ritim sensörü ise*/
            if (mThing.equals(mRateThing)) {
                /*MainActivity arayüzünde bulunan kalp ritim değerini üreten SeekBar kontrolüne erişim sağlanır.*/
                SeekBar seekBarRate = activity.findViewById(R.id.heartRateSensor);

                /*SeekBar'dan gelen veri ThingData nesnesine eklenir.*/
                mThingData.addData(seekBarRate.getProgress());
            }

            /*ThingData nesnesinde bulunan veri IoT-Ignite ortamına gönderilir.*/
            if (mThing.sendData(mThingData)) {
                Log.i(TAG, "VERİ GÖNDERİMİ BAŞARILI : " + mThingData);
            } else {
                Log.i(TAG, "VERİ GÖNDERİMİ BAŞARISIZ");
            }
        }
    }

    /*Thing bileşenlerden veri okumayı sağlayan görevi durdurmayı sağlar.
    Özellikle Thing bileşen ignite ortama kayıtlı olmadığında veya yeni gelen
    bir konfigürasyon bilgisini alırken  veri okumanın bu metot ile durdurulması
    gerekiyor. Parametre olarak nodeId ve thingId bilgilerini vermemiz gerekiyor.*/
    public void stopReadDataTask(String nodeId, String sensorId) {
        /*nodeId ve thingId bilgileri birleştirilerek key bilgi elde edilir.*/
        String key = nodeId + "|" + sensorId;

        /*Task nesnesi gelen key bilgisini içeriyorsa if bloğu çalışır.*/
        if (tasks.containsKey(key)) {
            try {
                /*Task içinde belirtilen key bilgisine sahip olan görev iptal edilir.*/
                tasks.get(key).cancel(true);

                /*Task içinde belirtilen key bilgisine sahip olan görev silinir.*/
                tasks.remove(key);
            } catch (Exception e) {
                Log.d(TAG, "Veri alma durdurulamadı" + e);
            }
        }
    }

    /********SENSÖR VERİLERİNİ IGNITE ORTAMINA GÖNDERMEK********/
    /*Ignite ortamına bağlantı sağlandığı zaman aşağıdaki metot çağrılır.
    Burada sensör için ilk değer ataması gerçekleşir. */
    private void sendInitialData() {
        /*sendData(): Bu metot ID bilgisi verilen Thing bileşenlere değer atamak için kullanılır.*/
        sendData(Constants.HEART_RATE_SENSOR, Constants.FIRST_VALUE_FOR_HEART_RATE);

    }

    /*Ignite ortamına veri göndermeyi sağlar. Bu işlemin yapılabilmesi için getDataReadingFrequency()
    metodu READING_WHEN_ARRIVE değerine sahip olmalıdır.*/
    public void sendData(String thingId, int value) {

        /*Ignite bağlantısı varsa*/
        if (igniteConnected) {
            try {

                /*Node ID bilgisi Ignite ortamından alınır.*/
                Node mNode = mIotIgniteManager.getNodeByID(Constants.NODE_ID);

                /*Node null değilse işlem yapılır.*/
                if (mNode != null) {

                    /*Node'a bağlı olan ve thingId ID bilgisine sahip olan Thing nesnesi alınır.*/
                    Thing mThing = mNode.getThingByID(thingId);

                    /*Thing null değilse işlem yapılır.*/
                    if (mThing != null) {

                        /*ThingData: IoT-Ignite ortamına Thing verilerini göndermek için kullanılır.
                        Her Thing nesnesi veri göndermek veya aktüatörler gibi eylem mesajı almak
                        için bir ThingData nesnesine ihtiyaç duyar.*/
                        ThingData mthingData = new ThingData();

                        /*Gelen veri ThingData nesnesine eklenir.*/
                        mthingData.addData(value);

                        /*getDataReadingFrequency() metodu READING_WHEN_ARRIVE değerini üretiyorsa Ignite platformuna veri gönderilebilir.
                        Aksi durumda gönderilemez*/
                        if (isConfigReadWhenArrive(mThing) && mThing.sendData(mthingData)) {
                            Log.i(TAG, "VERİ GÖNDERİMİ BAŞARILI : " + mthingData);
                        } else {
                            Log.i(TAG, "VERİ GÖNDERİLEMEDİ!");
                        }
                    } else {
                        Log.i(TAG, thingId + " kayıtlı değil!");
                    }
                } else {
                    Log.i(TAG, Constants.NODE_ID + " kayıtlı değil!");
                }
            } catch (AuthenticationException e) {
                Log.i(TAG, "AuthenticationException!");
            }
        } else {
            Log.i(TAG, "Ignite Bağlantısı Kesildi!");
        }
    }

    /*getDataReadingFrequency() metodu READING_WHEN_ARRIVE değerini üretiyorsa Ignite platformuna veri gönderilebilir.
      Aksi durumda gönderilemez*/
    private boolean isConfigReadWhenArrive(Thing mThing) {
        if (mThing.getThingConfiguration().getDataReadingFrequency() == ThingConfiguration.READING_WHEN_ARRIVE) {
            return true;
        }
        return false;
    }

}