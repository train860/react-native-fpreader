/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package cn.pda.serialport;

import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SerialPort {

	private static final String TAG = "SerialPort";
	
	
	private  static int TNCOM_EVENPARITY = 0;//żУ��
	private  static int TNCOM_ODDPARITY = 1 ;//��У��

	/*
	 * Do not remove or rename the field mFd: it is used by native method close();
	 */
	private FileDescriptor mFd;
	private FileInputStream mFileInputStream;
	private FileOutputStream mFileOutputStream;
	private boolean trig_on=false;
	byte[] test;
	public SerialPort(){} ;
	public SerialPort(int port, int baudrate, int flags) throws SecurityException, IOException {
		
//		mFd = open(port, baudrate);

		mFd = open(port, baudrate, TNCOM_EVENPARITY);
		
		if (mFd == null) {
			Log.e(TAG, "native open returns null");
			throw new IOException();
		}
		
		
		mFileInputStream = new FileInputStream(mFd);
		mFileOutputStream = new FileOutputStream(mFd);
		


	}

	// Getters and setters
	public InputStream getInputStream() {
		return mFileInputStream;
	}

	public OutputStream getOutputStream() {
		return mFileOutputStream;
	}
	public void zigbee_poweron() {
		zigbeepoweron();
	}
	public void printer_poweron() {

	}
	public void printer_poweroff() {

	}
	public void psam_poweron() {
		psampoweron();
	}
	public void psam_poweroff() {
		psampoweroff();
		//scaner_trigoff();
	}
	public void scaner_poweron() {
		scanerpoweron();
		scaner_trigoff();
	}
	public void scaner_poweroff() {
		scanerpoweroff();
		//scaner_trigoff();
	}
	public void scaner_trigon() {
		scanertrigeron();
		trig_on=true;
	}
	public void scaner_trigoff() {
		scanertrigeroff();
		trig_on=false;
	}
	public boolean scaner_trig_stat(){
		return trig_on;
	}
//	/**
//	 * @param mode  
//	 */
//	public void setPortparity(int mode){
//		setPortParity(mode);
//	}
	
	// JNI
	
	private native static FileDescriptor open(int port, int baudrate);
	private native static FileDescriptor open(int port, int baudrate, int portparity);
	public native void close(int port);
	public native void zigbeepoweron();
	public native void zigbeepoweroff();
	
	public native void scanerpoweron();
	public native void scanerpoweroff();
	public native void psampoweron();
	public native void psampoweroff();
	public native void scanertrigeron();
	public native void scanertrigeroff();
	public native void power3v3on();
	public native void power3v3off();
	
	public native void rfidPoweron();
	public native void rfidPoweroff();
	
	public native void usbOTGpowerOn();
	public native void usbOTGpowerOff();
	
	public native void irdapoweron();
	public native void irdapoweroff();
	
	public native void setGPIOhigh(int gpio);
	public native void setGPIOlow(int gpio);
	
	
//	public native void setPortParity(int mode);
	
	public native void test(byte[] bytes);
	
	static {
		System.loadLibrary("devapi");
		System.loadLibrary("irdaSerialPort");
	}
	
}
