package com.xrbpowered.utils;

public class TaskInterruptedException extends RuntimeException {

	public static void check() {
		if(Thread.interrupted())
			throw new TaskInterruptedException();
	}

}
