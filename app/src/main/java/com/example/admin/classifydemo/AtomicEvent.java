package com.example.admin.classifydemo;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * �ṩԭ�Ӳ�������Ҫ����ͬ���̣߳������߳�
 * 
 * @author Administrator
 * 
 */
public class AtomicEvent {

	// AtomicInteger atomicInteger = new AtomicInteger(0);
	AtomicInteger atomicInteger;
	String extra = null;

	public AtomicEvent() {
		atomicInteger = new AtomicInteger(0);
	}

	public AtomicEvent(int initialValue) {
		atomicInteger = new AtomicInteger(initialValue);
	}

	public void Reset() {
		atomicInteger.set(0);
	}

	public void set(int i) {
		atomicInteger.set(i);
	}

	public int get() {
		return atomicInteger.get();
	}

	public int getAndIncrement() {
		return atomicInteger.getAndIncrement();
	}

	public void Wait() throws InterruptedException {
		while (atomicInteger.get() == 0) {
			Thread.sleep(500);
		}
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public String getExtra() {
		return this.extra;
	}

	public boolean Wait(long mills) throws InterruptedException {
		int i = 0;
		while (atomicInteger.get() == 0) {
			Thread.sleep(100);
			i++;
			if ((i * 100) >= mills) {
				return false;
			}
		}
		return true;
	}

	public boolean Wait(long mills, long extra) throws InterruptedException {
		int i = 0;
		while (atomicInteger.get() == 0) {
			Thread.sleep(extra);
			i++;
			if ((i * extra) >= mills) {
				return false;
			}
		}
		return true;
	}
	
}
