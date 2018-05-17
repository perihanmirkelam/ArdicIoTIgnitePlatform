package com.mas.iotignitehealth;

import android.content.Intent;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

/*WearableListenerService: Bu sınıf, veri katmanı olaylarını (data layer event) bir
servis içinde dinlemeyi sağlar. Servisin yaşam döngüsü sistem tarafından yönetilir.
Veri öğeleri ve mesajları göndermek istediğinizde servise bağlanılır, aksi durumda
yani ihtiyaç olmayan durumlarda bağlantı kesilir. Servislerle çalışmak için bu sınıf
kullanılır.*/
public class ListenerService extends WearableListenerService {

    /* onDataChanged(): Data Layer da meydana gelen veri değişiklikleri algılayan
    metodumuz */
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        /*Akıllı saatte veri olaylarını kontrole eden döngümüz*/
        for (DataEvent event : dataEvents) {

            /*DataEvent.TYPE_CHANGED: Data Layer üzerinde veri değişikliği
            olduğunda (örneğin data layer’a yeni bir veri eklendiğin) yapılacak
            işlemleri belirlemede kullanılır.*/
            if (event.getType() == DataEvent.TYPE_CHANGED) {

                /*Verinin path bilgisi alınır ve değişkene atanır.*/
                String path = event.getDataItem().getUri().getPath();

                /*Path bilgisi /wearable_data ise, akıllı telefondan metin
                gönderildiği anlaşılır.*/
                if (path.equalsIgnoreCase("/wearable_data")) {

                    /*DataItem yapısında gönderilen metnimizi DataMap
                    nesnesine atarız*/
                    DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

                    /*Yeni bir intent oluşturduk. Bu intent ile NameActivity
                    isimli etkinlik başlatılacaktır.*/
                    Intent i = new Intent(this, MainActivity.class);

                    /*Aldığımız DataMap verisini intent içerisine ekleriz.
                    DataMap doğrudan eklenemediği için toBundle() metodu ile
                    veri Bundle nesnesine dönüştürülür. NameActivity isimli
                    etkinlikte veriyi almak için “datamap” key değerini kullanırız.*/
                    i.putExtra("data", dataMap.toBundle());

                    /*Etkinlik için bayrak tanımladık. Her veri değişiminde
                    etkinlik yeniden başlatılır ve gelen yeni veriyi kullanıcıya
                    gösterir FLAG_ACTIVITY_CLEAR_TASK: Çalışan bir
                    görev varsa temizlemeyi sağlar. FLAG_ACTIVITY_NEW_
                    TASK: Etkinliği yeni bir görev içinde başlatır.*/
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

                    /*Etkinlik başlatılır.*/
                    startActivity(i);
                }

            }
        }
    }
}
