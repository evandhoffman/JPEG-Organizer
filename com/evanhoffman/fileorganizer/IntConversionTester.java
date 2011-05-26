package com.evanhoffman.fileorganizer;

public class IntConversionTester {

	static final int SECONDS_PER_DAY = 86400;
	static final int MILLISECONDS_PER_SECOND = 1000;
	static final long A = 90 * SECONDS_PER_DAY * MILLISECONDS_PER_SECOND;
	static final long B = 90L * SECONDS_PER_DAY * MILLISECONDS_PER_SECOND;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("A: "+A);
		System.out.println("B: "+B);
	}

}
