package com.mas.basiccustomerapp;

/*Uygulamada kullanacağımız sabitleri tanımladığımı sınıfımız*/
public class Constants {

    /*Kurucu metodumuz*/
    private Constants() {

    }

    /*Sanal sensörler için VENDOR_INFO bilgisi*/
    public static final String VENDOR_INFO = "Basic Customer App";

    /*Sanal node ismi*/
    public static final String NODE_NAME = "VirtualCustomerNode1";

    /*Sanal sensör ismi*/
    public static final String TEMP_NAME = "Temperature";

    /*Sanal actuator ismi*/
    public static final String LAMP_NAME = "Lamp";

    /*Temperature sensörü için ilk değer*/
    public static final int FIRST_VALUE_FOR_TEMP = 21;

    /*Lamp actuatoru için ilk değer*/
    public static final int FIRST_VALUE_FOR_LAMP = 1;

}
