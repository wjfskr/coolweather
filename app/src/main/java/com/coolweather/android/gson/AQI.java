package com.coolweather.android.gson;
/*
* GSON解析映射类
* 当前空气质量
* */
public class AQI {
    public AQICity city;
    public class AQICity{
        public String aqi;
        public String pm25;
    }
}
