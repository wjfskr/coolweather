package com.coolweather.android;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    public DrawerLayout drawerLayout;//nav菜单
    public Button navButton;//nav按钮
    public SwipeRefreshLayout swipeRefresh;//下拉刷新天气（手动更新）
    private String mWeatherId;//城市天气id
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ImageView bingPicImg;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //可以使用MaterialDesign去实现背景图和状态栏融合，麻烦
        //Android5.0以上可以用此方法实现
        if (Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();//获取DecorView实例控件
            //活动布局显示在状态栏上
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);//状态栏设置为透明
        }
        setContentView(R.layout.activity_weather);

        //初始化各控件
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);//总的天气布局
        titleCity = (TextView) findViewById(R.id.title_city);//城市名(标题)
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);//天气的更新时间
        degreeText = (TextView)findViewById(R.id.degree_text);//当天气温
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);//当天概况
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);//未来天气信息
        aqiText = (TextView) findViewById(R.id.aqi_text);//天气质量标准1
        pm25Text = (TextView) findViewById(R.id.pm25_text);//天气质量标准2
        comfortText = (TextView) findViewById(R.id.comfort_text);//生活建议--舒适度
        carWashText = (TextView) findViewById(R.id.car_wash_text);//生活建议--洗车指数
        sportText = (TextView) findViewById(R.id.sport_text);//生活建议--运动建议
        bingPicImg = (ImageView)  findViewById(R.id.bing_pic_img);//天气背景图片
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);//下拉刷新天气
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);//刷新条颜色
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);//nav菜单
        navButton = (Button) findViewById(R.id.nav_button);//nav按钮

        //加载缓存（实现若用户选择过城市，则用户无需再次选择）
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        final String weatherId;//记录城市id(仅一次)
        if (weatherString != null){//有缓存时，直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            weatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        }else {
            //获取从列表传过来的城市id
            weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);//无缓存，将ScrollView隐藏
            requestWeather(weatherId);//根据天气id请求城市天气信息
        }
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(weatherId);
            }
        });


        //每日一图（同上，用缓存的方式，避免重复加载）
        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else {
            loadBingPic();//如果没有缓存过就去获取
        }

        //nav按钮就是打开，nav
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

    }
    /**
     * 根据天气id请求城市天气信息
     */
    public void requestWeather(final String weatherId){
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=784ba8fc12dd4a4ca4bdaa4a178414f3";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);//刷新结束隐藏进度条
                    }
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String responseText = response.body().string();
                //将JSON数据转换为Weather对象
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {//切换到主线程
                    @Override
                    public void run() {
                        //如果status为ok说明请求天气成功
                        if (weather != null && "ok".equals(weather.status)){//P514
                            //将JSON数据（String）缓存到SharedPreferences
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();//文件存储
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);//处理并展示Weather实体类中的数据
                        }else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);//刷新结束隐藏进度条
                    }
                });
            }
        });
        loadBingPic();
    }

    /**
     * 处理并展示Weather实体类中的数据
     */
    private void showWeatherInfo(Weather weather) {
        //从对象中获取数据
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "C";
        String weatherInfo = weather.now.more.info;
        //设置内容
        titleCity.setText(cityName);//城市标题
        titleUpdateTime.setText(updateTime);//更新时间
        degreeText.setText(degree);//气温
        weatherInfoText.setText(weatherInfo);//当天天气概况
        forecastLayout.removeAllViews();//现将未来几天的布局移除（先移除后添加，以此达到更新的目的）
        for (Forecast forecast : weather.forecastList) {//利用循环来处理每天的天气
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);//动态加载布局
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);//动态加入布局，书中P515
        }
        if (weather.aqi != null) {//空气质量
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度" + weather.suggestion.comfort.info;
        String carWash = "" + weather.suggestion.carWash.info;
        String sport = "" + weather.suggestion.sport.info;
        comfortText.setText(comfort);//生活建议--舒适度
        carWashText.setText(carWash);//生活建议--洗车指数
        sportText.setText(sport);//生活建议--运动建议
        weatherLayout.setVisibility(View.VISIBLE);//将ScollView显示
    }
/**
 * 加载必应每日一图
 */
    private void loadBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }
 }
