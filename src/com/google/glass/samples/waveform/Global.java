package com.google.glass.samples.waveform;

/**
 *	Stores all the variables that will be static during runtime
 *	@author Adrian Jimenez-Galindo
 */

public class Global {
	
	//Variables that store general information
	private static Boolean clicked = Boolean.valueOf(false);
	private static Boolean connected = Boolean.valueOf(false);
	private static String address = "0";
	
	/*
	* index and v_index are used to obtain the current horizontal
	* and vertical position of Glass
	*/
	private static int index = 0;
	private static int v_index = 0;

	// Variables to track position of the Glass
	private static float[] mRotationMatrix = new float[16];
	private static float[] mOrientation = new float[3];
	private static float[] matrix = new float[16];
	private static float[] gyro = new float[3];
	private static float heading;
	private static float pitch;
	
	// Getters and setters for variables
	public static void setAddress(String s) {
		address = s;
	}

	public static String getAddress() {
		return address;
	}

	public static int getIndex() {
		return index;
	}

	public static void setIndex(int index) {
		Global.index = index;
	}
	
	public static void setVindex(int index) {
		Global.v_index = index;
	}
	
	
	public static int getVindex() {
		return v_index;
	}
	
	/**
	 * Lets Global know that the sensor has been connected.
	 */
	public static void Connected() {
		connected = Boolean.valueOf(true);
	}
	/**
	 * Lets Global know that the sensor has been disconnected.
	 */
	public static void Disconnected() {
		connected = Boolean.valueOf(false);
	}
	/**
	 * @return false if sensor is disconnected, true otherwise.
	 */
	public static Boolean getConnected() {
		return Boolean.valueOf(connected);
	}
	
	/**
	 * @return true if the touch pad has been clicked (on to history mode), false otherwise.
	 */
	public static Boolean getClicked() {
		return Boolean.valueOf(clicked);
	}

	/**
	 * Register clicking of the touch pad.
	 */
	public static void onClick() {
		clicked = Boolean.valueOf(!clicked);
	}

	// Getters for position values.
	public static float[] getGyro() {
		return gyro;
	}
	
	public static float[] getMatrix() {
		return matrix;
	}
	
	public static float[] getRotationMatrix() {
		return mRotationMatrix;
	}

	public static float[] getOrientation() {
		return mOrientation;
	}

	public static float getHeading() {
		return heading;
	}

	public static float getPitch() {
		return pitch;
	}
	
	/**
	 * Takes a vector @param vec and extracts the corresponding raw heading and pitch of
	 * the vector. 
	 * Heading is processed as heading = -2*(toDegrees(value of vector)), for heading in range [-360, -25) U (25, 360].
	 * Pitch is rounded and negated: -1*(round(value of vector)), for pitch in range (-30, 40).
	 * Both values are floats.
	 * 
	 * @param vec : Ideally, the rotation vector obtained from the gyroscope.
	 */
	public static void setHeadingAndPitch(float[] vec) {
		heading = -2*(float)Math.toDegrees(vec[0]);
		//System.out.println(heading);
		pitch = Math.round(-1*(float) Math.toDegrees(vec[1]));
		
		if (Math.abs(heading) <= 25) {
			heading = 0;
		}
		if (pitch >= 40) {
			pitch = 40;
		}
		if (pitch <= -30) {
			pitch = -30;
		}

	}

	/**
	 * Add @param y to the end of the @param array, shifting all values to the 
	 * beginning
	 * 
	 * @param y : the value to be added to the array
	 * @param array : the array
	 */
	public static void Push(float y, float[] array) {
		for (int i = 0; i < array.length - 1; i++) {
			array[i] = array[i + 1];
		}
		array[array.length - 1] = y;
	}
	
	/**
	 * Returns all values to its default value
	 */	
	public static void resetGlobals() {
		clicked = Boolean.valueOf(false);
		index = 0;
		v_index = 0;
		address = "0";
	}
}