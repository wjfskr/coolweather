package com.coolweather.android.db;
/**
 * 存放省信息
 */

import org.litepal.crud.LitePalSupport;

public class Province extends LitePalSupport {//Litepal中的每个实体类必须继承DataSupport类
    private int id;//每个实体类中自己的字段
    private String provinceName;//记录省的名字
    private int provinceCode;//记录省的代号
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getProvinceName() {
        return provinceName;
    }
    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }
    public int getProvinceCode() {
        return provinceCode;
    }
    public void setProvinceCode(int provinceCode) {
        this.provinceCode = provinceCode;
    }
}

