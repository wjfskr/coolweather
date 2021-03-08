package com.coolweather.android.util;

import android.text.TextUtils;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.gson.Weather;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utility {
    /**
     * 解析和处理服务器返回的省级数据（存数据库对应表）
     */
    public static boolean handleProvinceResponse(String response){
        if (!TextUtils.isEmpty(response)){//判断reponse字符串是否为空
            try {
                JSONArray allProvinces = new JSONArray(response);
                for (int i = 0; i < allProvinces.length(); i++){
                    JSONObject provinceObject = allProvinces.getJSONObject(i);
                    //数据库存储
                    Province province = new Province();
                    province.setProvinceName(provinceObject.getString("name"));//使用JSONObject解析(书中P330)
                    province.setProvinceCode(provinceObject.getInt("id"));//使用LitePal添加数据(书中P236)
                    province.save();
                }
                return true;
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        return false;
    }
    /**
     * 解析和处理服务器返回的市级数据（存数据库对应表）
     */
    public static boolean handleCityResponse(String reponse, int provinceId){
        if (!TextUtils.isEmpty(reponse)){
            try {
                JSONArray allCities = new JSONArray(reponse);
                for (int i = 0; i < allCities.length(); i++){
                    JSONObject cityObject = allCities.getJSONObject(i);
                    //数据库存储
                    City city = new City();
                    city.setCityName(cityObject.getString("name"));
                    city.setCityCode(cityObject.getInt("id"));
                    city.setProvinceId(provinceId);//所在市的省的id
                    city.save();
                }
                return true;
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        return false;
    }
    /**
     * 解析和处理服务器返回的县级数据（存数据库对应表）
     */
    public static boolean handleCountResponse(String response, int cityId){
        if (!TextUtils.isEmpty(response)){
            try {
                JSONArray allCounties = new JSONArray(response);
                for (int i = 0; i < allCounties.length(); i++){
                    JSONObject countyObject = allCounties.getJSONObject(i);
                    County county = new County();
                    county.setCountyName(countyObject.getString("name"));
                    county.setWeatherId(countyObject.getString("weather_id"));
                    county.setCityId(cityId);
                    county.save();
                }
                return true;
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 将返回的JSON数据解析成Weather实体类
     */

    public static Weather handleWeatherResponse(String response){
        try {
            JSONObject jsonObject = new JSONObject(response);//书中P331
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather");//解析出天气主体
            String weatherContent = jsonArray.getJSONObject(0).toString();//书中P332
            return new Gson().fromJson(weatherContent, Weather.class);//将JSON数据转化为Weather对象
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
