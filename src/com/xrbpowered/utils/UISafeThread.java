package com.xrbpowered.utils;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

public class UISafeThread extends Thread {

	private Runnable uiRunnable = new Runnable() {
		@Override
		public void run() {
			uiRun();
		}
	};
	
	@Override
	public void run() {
		try {
			for(;;) loop();
		}
		catch(InterruptedException e) {
		}
	}
	
	public void safeUIRun() throws InterruptedException {
		try {
			SwingUtilities.invokeAndWait(uiRunnable);
		} catch(InvocationTargetException e) {
			e.getCause().printStackTrace();
		}
	}

	public void safeUIRunAsync() {
		SwingUtilities.invokeLater(uiRunnable);
	}

	protected void loop() throws InterruptedException {
	}
	
	protected void uiRun() {
	}

}
