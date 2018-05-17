package com.mas.iotignitehealth;

/*Uygulamada kullanacağımız sabitleri tanımladığımı sınıfımız*/
public class Constants {

    /*Kurucu metodumuz*/
    private Constants() {

    }

    /*Sanal sensörler için VENDOR_INFO bilgisi*/
    public static final String VENDOR_INFO = "Health App";

    /*Sanal node ismi*/
    public static final String NODE_NAME = "HumanNode";

    /*Sanal sensör ismi*/
    public static final String HEART_RATE_SENSOR = "HeartRateSensor";


    /*Heart Rate sensörü için ilk değer*/
    public static final int FIRST_VALUE_FOR_HEART_RATE = 80;


}
