package com.sip.fpreader;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;

import com.IDWORLD.LAPI;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cn.pda.serialport.SerialPort;


/**
 * Created by train on 2018/11/15.
 */

public class FPReaderModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private ReactApplicationContext mContext;
    private SerialPort mSerialport ;
    private LAPI m_cLAPI = null;
    private int m_hDevice = 0;
    private byte[] m_image = new byte[LAPI.WIDTH*LAPI.HEIGHT];

    public static final int MSG_ERROR = 101;
    public static final int MSG_IMAGE = 102;
    public static final int MSG_TEMPLATE = 103;
    public static final int MSG_COMPARE = 104;


    public FPReaderModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mContext=reactContext;
    }

    @Override
    public String getName() {
        return "FPReaderModule";
    }
    @ReactMethod
    public void readData(){
        if(m_cLAPI==null){
            m_cLAPI = new LAPI(mContext.getCurrentActivity());
        }

        Runnable r = new Runnable() {
            public void run() {
                if(mSerialport==null){
                    mSerialport = new SerialPort();
                    mSerialport.setGPIOlow(141);
                    try {
                        Thread.sleep(1000) ;
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                getImage ();
            }
        };
        Thread s = new Thread(r);
        s.start();
    }
    @ReactMethod
    public void close(){
        Runnable r = new Runnable() {
            public void run() {
                closeDevice ();
                if(mSerialport != null){
                    mSerialport.setGPIOhigh(141) ;
                }else{
                    mSerialport = new SerialPort();
                    mSerialport.setGPIOlow(141);
                }

            }
        };
        Thread s = new Thread(r);
        s.start();
    }

    protected boolean openDevice()
    {
        if(m_hDevice!=0){
            return true;
        }
        m_hDevice = m_cLAPI.OpenDeviceEx();
        if (m_hDevice==0) {
            String msg = "Can't open device !";
            m_fEvent.sendMessage(m_fEvent.obtainMessage(MSG_ERROR,0, 0,msg));
            return false;
        }
        return true;
    }

    protected void closeDevice()
    {

        if (m_hDevice == 0) return;
        m_cLAPI.CloseDeviceEx(m_hDevice);
        m_hDevice = 0;

    }

    protected void getImage()
    {
        if(!openDevice()){
            if(m_fEvent!=null){
                m_fEvent.sendMessage(m_fEvent.obtainMessage(MSG_ERROR,0, 1,"设备打开失败"));
            }
            return;
        }
        int ret;

        ret = m_cLAPI.GetImage(m_hDevice, m_image);
        if (ret != LAPI.TRUE){
            if(m_fEvent!=null){
                m_fEvent.sendMessage(m_fEvent.obtainMessage(MSG_ERROR,0, 1,"获取指纹图片失败"));
            }

        }
        else {
            String temp=createTemp(m_image);
            String img=bitmapToBase64(fingerBitmap(m_image,LAPI.WIDTH, LAPI.HEIGHT));
            Map<String,String> map=new HashMap<>();
            map.put("template",temp);
            map.put("image",img);
            if(m_fEvent!=null){
                m_fEvent.sendMessage(m_fEvent.obtainMessage(MSG_IMAGE, map));
            }

        }

    }

    // 生成指纹特征值
    private String createTemp(byte[] image)
    {
        byte[] m_itemplate = new byte[LAPI.FPINFO_STD_MAX_SIZE];
        int ret=m_cLAPI.CreateTemplate(m_hDevice,image,m_itemplate);
        if(ret==0){
            return null;
        }
        String msg="";
        for (int i=0; i < LAPI.FPINFO_STD_MAX_SIZE; i ++) {
            msg += String.format("%02x", m_itemplate[i]);
        }
        return msg;
    }

    private int compareTemps(byte[] temp1,byte[] temp2)
    {
        return m_cLAPI.CompareTemplates(m_hDevice,temp1, temp2);
    }

    private Bitmap fingerBitmap(byte[] image, int width, int height) {
        if (width==0 ||height==0) return null;

        int[] RGBbits = new int[width * height];

        for (int i = 0; i < width * height; i++ ) {
            int v;
            if (image != null) v = image[i] & 0xff;
            else v= 0;
            RGBbits[i] = Color.rgb(v, v, v);
        }
        return Bitmap.createBitmap(RGBbits, width, height, Bitmap.Config.RGB_565);

    }
    private String bitmapToBase64(Bitmap bitmap) {

        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.NO_WRAP);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
    private final Handler m_fEvent = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            WritableMap params = Arguments.createMap();
            switch (msg.what) {
                case MSG_ERROR:

                    params.putString("error", (String) msg.obj);
                    break;
                case MSG_IMAGE:
                    Map data= (Map) msg.obj;
                    params.putString("image", String.valueOf(data.get("image")));
                    params.putString("template",String.valueOf(data.get("template")));
                    break;
                default:
                    break;
            }
            mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("FPReaderResult",params);
            super.handleMessage(msg);
        }
    };

    @Override
    public void onHostResume() {

    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {
        closeDevice();
        if(mSerialport != null){
            mSerialport.setGPIOhigh(141) ;
        }
    }
}
