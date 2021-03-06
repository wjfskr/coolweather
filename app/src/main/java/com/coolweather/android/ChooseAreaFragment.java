package com.coolweather.android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.jetbrains.annotations.NotNull;
import org.litepal.LitePal;
import org.litepal.crud.LitePalSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    private TextView titleText;
    private ListView listView;
    private Button backButton;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();//这个容器是暂时存放省或市或县，作为适配器的参数
    private ProgressDialog progressDialog;
    //选中状态
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    //省列表
    private List<Province> provinceList;
    //市列表
    private List<City> cityList;
    //县列表
    private List<County> countyList;
    //选中的省
    private Province selectedProvince;
    //选中的市
    private City selectedCity;
    //当前选中的级别
    private int currentLevel;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //加载布局，选择地区布局
        View view = inflater.inflate(R.layout.choose_area, container, false);
        //获取模拟ActionBar的布局控件实例
        titleText = (TextView) view.findViewById(R.id.title_text);//获得标题实例
        backButton = (Button) view.findViewById(R.id.back_button);//获得返回按钮实例
        listView = (ListView) view.findViewById(R.id.list_view);//获得列表实例
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);//初始化ArrayAdapter
        listView.setAdapter(adapter);//设为ListView适配器
        return view;
    }
    //活动与碎片相关联已经建立完毕时调用
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //列表的点击事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVINCE){ //当前页面加载的级别==省
                    selectedProvince = provinceList.get(position);//根据点击的位置得到所选择的省份
                    queryCities();//查询市（加载市级数据）
                }else if (currentLevel == LEVEL_CITY) {//当前页面加载的级别==市
                    selectedCity = cityList.get(position);//根据点击的位置得到所选择的城市
                    queryCounties();//查询县（加载县级数据）
                }
                else if (currentLevel == LEVEL_COUNTY){//当前页面加载的级别==县
                    String weatherId = countyList.get(position).getWeatherId();//根据点击的位置得到城市天气ID
                    //如果当前页面是在主页面那就要跳页，如果页面在天气页面(通过nav点开的)，就只需要更新数据
                    if(getActivity() instanceof MainActivity){
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);//启动WeatherActivity
                        intent.putExtra("weather_id", weatherId);//传值：天气的ID
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity){//此时是在nav中选择城市
                        //碎片中获取活动的方法
                         WeatherActivity  activity=(WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();//关闭nav
                        activity.swipeRefresh.setRefreshing(true);//开启刷新（只是个样）
                        activity.requestWeather(weatherId);//请求天气信息
                    }

                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel == LEVEL_COUNTY){//当前级别为县，返回到市中
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){//当前级别为市，返回到省中
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }
    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查
     */
    private void queryProvinces() {
        titleText.setText("中国");//头局部标题设为中国
        backButton.setVisibility(View.GONE);//将返回按钮隐藏
        provinceList = LitePal.findAll(Province.class);//从LitePal的查询接口读取省级数据
        if (provinceList.size() > 0){//读取到数据，直接显示在界面上
            dataList.clear();
            for (Province province : provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();//刷新列表
            listView.setSelection(0);//将列表滚动到顶部
            currentLevel = LEVEL_PROVINCE;//设目前查询状态为省
        }else {//没读取到数据,访问服务器
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");//从服务器上查询数据
        }
    }
    /**
     * 查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查
     */
    private void queryCities(){
        titleText.setText(selectedProvince.getProvinceName());//将被选中的省份作为标题
        backButton.setVisibility(View.VISIBLE);//返回按钮显示(因为目前已经到City,可以返回到省级）
        cityList = LitePal.where("provinceid = ?",String.valueOf(selectedProvince.getId()))
                .find(City.class);//根据省条件筛选市
        if (cityList.size() > 0){ //从数据库读到数据
            dataList.clear();
            for (City city : cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else {//没读取到数据,访问服务器
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }
    /**
     * 查询选中市内所有的县，优先从"数据库"查询，如果没有查询到再去服务器上查
     */
    private void queryCounties(){
        titleText.setText(selectedCity.getCityName());//将被选中的市份作为标题
        backButton.setVisibility(View.VISIBLE);//返回按钮显示(因为目前已经到County,可以返回到市级）
        countyList = LitePal.where("cityid = ?",String.valueOf(selectedCity.getId()))
                .find(County.class);//根据市条件筛选县
        if (countyList.size() > 0){//从数据库读到数据
            dataList.clear();
            for (County county : countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }else {//没读取到数据,访问服务器
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
        }
    }
    /**
     * 根据传入的地址和类型从"服务器"上查询省市县数据
     */
    private void queryFromServer(String address, final String type) {
        showProgressDialog();//显示对话框
        HttpUtil.sendOkHttpRequest(address, new Callback() {//向服务器发送请求
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();//关闭对话框
                            Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                        }
                    });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String responseText = response.body().string();//具体返回的数据
                boolean result = false;

                if ("province".equals(type)){
                    //解析数据并存于数据库
                    result = Utility.handleProvinceResponse(responseText);//解析和处理服务器返回的数据Utility中的方法
                }else if ("city".equals(type)){
                    //解析数据并存于数据库
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());//获取城市所在省的id
                }else if ("county".equals(type)){
                    //解析数据并存于数据库
                    result = Utility.handleCountResponse(responseText, selectedCity.getId());
                }

                if (result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();//关闭对话框
                            if ("province".equals(type)){
                                queryProvinces();//再次开启查询，此次可能因为访问服务器数据，存于数据库，而成功查取
                            }else if ("city".equals(type)){
                                queryCities();
                            }else if ("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog(){
        if (progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());//书中P348
            progressDialog.setMessage("正在加载...");//设置提示信息
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
    /**
     * 关闭对话框
     */
    private void closeProgressDialog(){
        if (progressDialog != null){
            progressDialog.dismiss();
        }
    }
}
