package com.example.admin.classifydemo;

/**
 * Created by admin on 2017/11/23.
 */

class MyTimeoutException extends Exception {
    private static final long serialVersionUID = 1L;

    int m_type;

    // 异常信息描述
    public MyTimeoutException(String errorMsg){
        super(errorMsg);
        m_type = 0;
    }
    // 异常信息描述
    public MyTimeoutException(String errorMsg,int type ){
        super(errorMsg);
        m_type = type;
    }

}
