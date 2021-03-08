package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;
/*
 * GSON解析映射类
 * */
public class Weather {
    public String status;
    public Basic basic;
    public AQI aqi;
    public Now now;
    public Suggestion suggestion;
    @SerializedName("daily_forecast")
    public List<Forecast> forecastList;//由于预测是在JSON数据中是数组形式（不止一天）
}
