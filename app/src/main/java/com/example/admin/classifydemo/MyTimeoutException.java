package com.example.admin.classifydemo;

/**
 * Created by admin on 2017/11/23.
 */

class MyTimeoutException extends Exception {
    private static final long serialVersionUID = 1L;

    // 异常信息描述
    public MyTimeoutException(String errorMsg){
        super(errorMsg);
    }
}
