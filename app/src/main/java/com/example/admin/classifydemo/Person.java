package com.example.admin.classifydemo;

/**
 * Created by admin on 2017/11/24.
 */

public class Person {

    private String phone;
    private String info;
    private int status;

    public Person(String phone, String info, int status){
        this.phone = phone;
        this.info = info;
        this.status = status;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
