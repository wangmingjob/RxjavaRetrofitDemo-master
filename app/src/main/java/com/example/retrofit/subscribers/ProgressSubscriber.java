package com.example.retrofit.subscribers;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import com.example.retrofit.entity.api.BaseApi;
import com.example.retrofit.http.cookie.CookieDbUtil;
import com.example.retrofit.http.cookie.CookieResulte;
import com.example.retrofit.listener.HttpOnNextListener;

import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

import rx.Observable;
import rx.Subscriber;

/**
 * 用于在Http请求开始时，自动显示一个ProgressDialog
 * 在Http请求结束是，关闭ProgressDialog
 * 调用者自己对请求数据进行处理
 * Created by WZG on 2016/7/16.
 */
public class ProgressSubscriber<T> extends Subscriber<T> {
    /*是否弹框*/
    private boolean showPorgress=true;
    //    回调接口
    private HttpOnNextListener mSubscriberOnNextListener;
    //    弱引用反正内存泄露
    private WeakReference<Context> mActivity;
    //    加载框可自己定义
    private ProgressDialog pd;
    /*请求数据*/
    private BaseApi api;


    /**
     * 构造
     * @param api
     */
    public ProgressSubscriber(BaseApi api){
        this.api=api;
        this.mSubscriberOnNextListener = api.getListener();
        this.mActivity = new WeakReference<>(api.getRxAppCompatActivity());
        setShowPorgress(api.isShowProgress());
        if(api.isShowProgress()){
            initProgressDialog(api.isCancel());
        }
    }


    /**
     * 初始化
     * @param mSubscriberOnNextListener
     * @param context
     * @param showPorgress 是否需要加载框
     * @param cancel 是否能取消加载框
     */
    public ProgressSubscriber(HttpOnNextListener mSubscriberOnNextListener, Context context,boolean showPorgress,boolean cancel) {
        this.mSubscriberOnNextListener = mSubscriberOnNextListener;
        this.mActivity = new WeakReference<>(context);
        setShowPorgress(showPorgress);
        if(showPorgress){
            initProgressDialog(cancel);
        }
    }


    /**
     * 初始化加载框
     */
    private void initProgressDialog(boolean cancel) {
        Context context = mActivity.get();
        if (pd == null && context != null) {
            pd = new ProgressDialog(context);
            pd.setCancelable(cancel);
            if (cancel) {
                pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        onCancelProgress();
                    }
                });
            }
        }
    }


    /**
     * 显示加载框
     */
    private void showProgressDialog() {
        if(!isShowPorgress())return;
        Context context = mActivity.get();
        if (pd == null || context == null) return;
        if (!pd.isShowing()) {
            pd.show();
        }
    }


    /**
     * 隐藏
     */
    private void dismissProgressDialog() {
        if(!isShowPorgress())return;
        if (pd != null && pd.isShowing()) {
            pd.dismiss();
        }
    }


    /**
     * 订阅开始时调用
     * 显示ProgressDialog
     */
    @Override
    public void onStart() {
        showProgressDialog();


    }

    /**
     * 完成，隐藏ProgressDialog
     */
    @Override
    public void onCompleted() {
        dismissProgressDialog();
    }

    /**
     * 对错误进行统一处理
     * 隐藏ProgressDialog
     *
     * @param e
     */
    @Override
    public void onError(Throwable e) {
        Context context = mActivity.get();
        if (context == null) return;
        dismissProgressDialog();
        /*需要緩存并且本地有缓存才返回*/
        if(api.isCache()){
            Observable.just(api.getUrl()).subscribe(new Subscriber<String>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    errorDo(e,context);
                }

                @Override
                public void onNext(String s) {
                    /*获取缓存数据*/
                    CookieResulte cookieResulte= CookieDbUtil.getInstance().queryCookieBy(s);
                    mSubscriberOnNextListener.onCacheNext(cookieResulte.getResulte());
                }
            });
        }else{
            errorDo(e,context);
        }
    }

    /*错误统一处理*/
    private void errorDo(Throwable e,Context context){
        if (e instanceof SocketTimeoutException) {
            Toast.makeText(context, "网络中断，请检查您的网络状态", Toast.LENGTH_SHORT).show();
        } else if (e instanceof ConnectException) {
            Toast.makeText(context, "网络中断，请检查您的网络状态", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "错误" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        if(mSubscriberOnNextListener!=null){
            mSubscriberOnNextListener.onError(e);
        }
    }

    /**
     * 将onNext方法中的返回结果交给Activity或Fragment自己处理
     *
     * @param t 创建Subscriber时的泛型类型
     */
    @Override
    public void onNext(T t) {
        if (mSubscriberOnNextListener != null) {
            mSubscriberOnNextListener.onNext(t);
        }
    }

    /**
     * 取消ProgressDialog的时候，取消对observable的订阅，同时也取消了http请求
     */
    public void onCancelProgress() {
        if (!this.isUnsubscribed()) {
            this.unsubscribe();
        }
    }


    public boolean isShowPorgress() {
        return showPorgress;
    }

    /**
     * 是否需要弹框设置
     * @param showPorgress
     */
    public void setShowPorgress(boolean showPorgress) {
        this.showPorgress = showPorgress;
    }
}