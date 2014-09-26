/*
 * Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.glass.samples.waveform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.glass.samples.waveform.Global;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.SurfaceView;

/**
 * A view that displays sensor data on the screen as a waveform, and allows
 * browsing through head movements.
 * 
 * @author Javier Hernandez and @author Adrián Jimenez-Galindo.
 */

//TODO: More intuitive controls (i.e., no left/right bobbing while zooming in/out),

public class WaveformView extends SurfaceView {
	private Paint mPaint;
	private Paint textPaint;
	private Paint erasePaint;

	public int count = 0;
	public float maxEDA = (float) 0.01;
	public float minEDA = 5;
	public float maxTEMP = (float) 0.01;
	public float minTEMP = 99;
	List<Integer> list_modes = null;
	int current_mode_pos = 0;
	int SR = 8;
	int max_time_window = 10;// maximum time window size -> should be the same
								// as the maximum of list_modes
	public double[] EDA;
	private double[] EDA_history;
	private double[] ACL_X;
	private double[] ACL_Y;
	private double[] ACL_Z;
	private double[] TEMP;
	public double battery = 0;

	private int zoom = 0;
	private int xoom = 0;
	private int temp_xoom = 0;
	private int temp_zoom = 0;
	private float left = 640; //initial values for a standard window
	private float right = 630.4167f;
	private float difference = 0;
	private float vdifference = 0;
	private int start, end;

	private int min_bin_size = 10;
	private Path mPath;

	double alpha = 1;// 0.2 exponential smoothing coefficient

	public WaveformView(Context context) {
		this(context, null, 0);
	}

	public WaveformView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public WaveformView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		// mAudioData = new double[];
		EDA = new double[max_time_window * SR * 60];
		EDA_history = new double[max_time_window * SR * 60];
		ACL_X = new double[max_time_window * SR * 60];
		ACL_Y = new double[max_time_window * SR * 60];
		ACL_Z = new double[max_time_window * SR * 60];
		TEMP = new double[max_time_window * SR * 60];

		// different zoom in/out windows
		list_modes = new ArrayList<Integer>();
		list_modes.add(10);
		list_modes.add(30);
		list_modes.add(60);
		list_modes.add(5 * 60);
		list_modes.add(max_time_window * 60);

		mPath = new Path();

		mPaint = new Paint();
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setColor(Color.WHITE);
		mPaint.setStrokeWidth(0);
		mPaint.setAntiAlias(true);

		textPaint = new Paint();
		textPaint.setStrokeWidth(0);
		textPaint.setColor(Color.WHITE);
		textPaint.setAntiAlias(true);

		erasePaint = new Paint();
		erasePaint.setStyle(Paint.Style.STROKE);
		erasePaint.setColor(Color.TRANSPARENT);
		erasePaint.setStrokeWidth(0);
		erasePaint.setAntiAlias(true);

	}

	public int getTimeMode() {
		return list_modes.get(current_mode_pos);
	}

	/**
	 * Process sensor reading
	 * 
	 * @param sensorReading
	 *            : Sensor reading.
	 */
	public synchronized void updateSensorData(String sensorReading) {
		String[] parts = sensorReading.split(",");

		// control event marks
		if (parts.length != 7) {
			sensorReading = "0,0,0,0,0,0,0";
			parts = sensorReading.split(",");
		}
		// Shift sensor readings -- akin to the push method in globals.
		for (int i = (EDA.length - 1); i >= 1; i--) {
			EDA[i] = EDA[i - 1];
			TEMP[i] = TEMP[i - 1];
			ACL_X[i] = ACL_X[i - 1];
			ACL_Y[i] = ACL_Y[i - 1];
			ACL_Z[i] = ACL_Z[i - 1];
		}
		EDA[0] = Double.parseDouble(parts[6]);
		if (EDA[0] < 0.01) {
			EDA[0] = 0;
		}
		TEMP[0] = Double.parseDouble(parts[5]);
		ACL_Z[0] = Double.parseDouble(parts[1]);
		ACL_Y[0] = Double.parseDouble(parts[2]);
		ACL_X[0] = Double.parseDouble(parts[3]);
		battery = Double.parseDouble(parts[4]);

		// Exponential Smoothing EDA
		EDA[0] = EDA[0] * alpha + (1 - alpha) * EDA[1];

		// Update EDA VALUES
		Float curr_maxEDA = (float) 0.01;
		Float curr_minEDA = (float) 9999;
		for (int i = 0; i < SR * getTimeMode(); i++) {
			if (EDA[i] > curr_maxEDA) {
				curr_maxEDA = (float) EDA[i];
			}

			if (EDA[i] < curr_minEDA) {
				curr_minEDA = (float) EDA[i];
			}
		}
		maxEDA = curr_maxEDA;
		minEDA = curr_minEDA;
		// Update TEMP VALUES
		Float curr_maxTEMP = (float) 0.01;
		Float curr_minTEMP = (float) 9999;
		for (int i = 0; i < SR * getTimeMode(); i++) {
			if (TEMP[i] > curr_maxTEMP) {
				curr_maxTEMP = (float) TEMP[i];
			}
			if (TEMP[i] < curr_minTEMP) {
				curr_minTEMP = (float) TEMP[i];
			}
		}
		maxTEMP = curr_maxTEMP;
		minTEMP = curr_minTEMP;

		// Update the display.
		Canvas canvas = getHolder().lockCanvas();
		if (canvas != null) {
			drawWaveform(canvas);
			getHolder().unlockCanvasAndPost(canvas);
		}
	}

	/**
	 * Main method to draw all the data on screen.
	 */
	private void drawWaveform(Canvas canvas) {
		// Clear the screen each time because SurfaceView won't do this for us.
		int colorDelta = 255 / (2);
		int brightness = colorDelta;
		float height = canvas.getHeight();
		float offset_y = height * 0.2f;

		mPaint.setStrokeWidth(5);
		canvas.drawColor(Color.BLACK);
		float minACL = -5;
		float maxACL = 5;
		int bin_size = 100;
		int left = 0;
		int right = SR * getTimeMode();
		mPaint.setColor(Color.WHITE);
		drawInterface(canvas, mPaint);

		// Store previous value of zoom and xoom.
		temp_zoom = zoom;
		temp_xoom = xoom;

		if (!Global.getClicked()) {

			zoom = 0;
			xoom = 0;

			mPaint.setColor(Color.argb(brightness, 128, 255, 192));
			drawLine(EDA, minEDA - 0.005f, maxEDA + 0.005f, (float) offset_y,
					height - (float) offset_y, mPaint, canvas, left, right,
					zoom);

			mPaint.setColor(Color.argb(brightness, 255, 125, 125));
			drawLine(ACL_X, minACL, maxACL, (float) (0), (float) (offset_y),
					mPaint, canvas, left, right, zoom);

			mPaint.setColor(Color.argb(brightness, 125, 255, 125));
			drawLine(ACL_Y, minACL, maxACL, (float) (0), (float) (offset_y),
					mPaint, canvas, left, right, zoom);

			mPaint.setColor(Color.argb(brightness, 125, 125, 255));
			drawLine(ACL_Z, minACL, maxACL, (float) (0), (float) (offset_y),
					mPaint, canvas, left, right, zoom);

		} else {

			// Detect current relative position (horizontal and vertical), and
			// works with the offset from there (difference / vdifference).
			// Useful
			// to avoid having an absolute zero.
			if (Global.getIndex() == 0) {
				Global.setIndex((int) Global.getHeading());
			}
			if (Global.getVindex() == 0) {
				Global.setVindex((int) Global.getPitch());
			}

			difference = Global.getHeading() - Global.getIndex();
			vdifference = Global.getPitch() - Global.getVindex();

			if (Math.abs(difference) <= 3) {
				difference = 0;
			}
			if (Math.abs(vdifference) <= 3) {
				vdifference = 0;
			}

			// Compute offset and onset --i.e., the left and right points of a
			// screen.
			int k = (int) (-2 * difference);
			if (k >= EDA_history.length - 1 - bin_size) {
				k = EDA_history.length - 1 - bin_size;
			}
			if (k <= 0) {
				k = 0;
			}
			int kend = k + bin_size;
			if (kend <= bin_size) {
				kend = bin_size;
			}
			if (kend >= EDA_history.length - 1) {
				kend = EDA_history.length - 1;
			}

			updateXoomZoom(k, kend, difference, vdifference);
			mPaint.setColor(Color.CYAN);

			drawMobileBox(canvas, k + xoom, kend + xoom, zoom, mPaint);

			mPaint.setColor(Color.argb(brightness, 128, 255, 192));
			drawLine(EDA_history, minEDA - 0.005f, maxEDA + 0.005f,
					(float) offset_y, height - (float) offset_y, mPaint,
					canvas, k + xoom, kend + xoom, zoom);

			mPaint.setColor(Color.argb(brightness, 255, 125, 125));
			drawLine(ACL_X, minACL, maxACL, (float) (0), (float) (offset_y),
					mPaint, canvas, left, right, 0);

			mPaint.setColor(Color.argb(brightness, 125, 255, 125));
			drawLine(ACL_Y, minACL, maxACL, (float) (0), (float) (offset_y),
					mPaint, canvas, left, right, 0);

			mPaint.setColor(Color.argb(brightness, 125, 125, 255));
			drawLine(ACL_Z, minACL, maxACL, (float) (0), (float) (offset_y),
					mPaint, canvas, left, right, 0);

		}
	}

	public void resetCurrentPosition() {
		Global.setIndex(0);
		Global.setVindex(0);
	}

	/**
	 * Plots data from @param signal.
	 * 
	 * @param signal
	 *            : the vector where the data will be plotted.
	 * @param minVal
	 *            : minimum value from the data (for scaling)
	 * @param maxVal
	 *            : maximum value from the data (for scaling)
	 * @param minY
	 *            : minimum value for the plotting area (for scaling)
	 * @param maxY
	 *            : maximum value for the plotting area (for scaling)
	 * @param mPaint
	 * @param canvas
	 * @param onset
	 *            : leftmost value for the screen (0 if not on history mode)
	 * @param offset
	 *            : rightmost value for the screen (sampling rate * time mode if
	 *            not history mode)
	 * @param ctrl
	 *            : current zoom
	 */
	private void drawLine(double[] signal, float minVal, float maxVal,
			float minY, float maxY, Paint mPaint, Canvas canvas, int onset,
			int offset, int ctrl) {

		float prev_x = -1;
		float prev_y = -1;
		float cur_x;
		float cur_y;

		start = onset;
		end = offset;

		// Check if offset and onset are within the allowed range of values when
		// in history mode
		if (Global.getClicked()) {

			// Maximum permitted data points onscreen: [19, signal.length - 1]
			if (offset <= 19) {
				offset = 19;
			}

			if (offset >= signal.length - 1) {
				offset = signal.length - 1;
			}

			if (onset <= 0) {
				onset = 0;
			}

			if (onset >= signal.length - 21) {
				onset = signal.length - 21;
			}

			// Vertical check. Minimum tolerated amount of data displayed is
			// 20.
			if (offset + ctrl <= onset - ctrl + 20) {
				ctrl = (onset - offset + 20) / 2;
			}

			// Declare start and end variables for plotting --useful since we
			// want them
			// to be independent from each other but both functions of ctrl.
			start = onset - ctrl;
			if (start <= 0) {
				start = 0;
			}
			if (start >= signal.length - 21) {
				start = signal.length - 21;
			}
			end = offset + ctrl;
			if (end <= 20) {
				end = 20;
			}
			if (end >= signal.length - 1) {
				end = signal.length - 1;
			}
		}

		for (int i = start; i < end; i++) {
			cur_x = canvas.getWidth()
					- scale_num(i, start, end, 1, canvas.getWidth());
			cur_y = canvas.getHeight()
					- scale_num((float) signal[i], minVal, maxVal, minY, maxY);

			if (prev_x != -1) {
				canvas.drawLine(prev_x, prev_y, cur_x, cur_y, mPaint);
			}
			prev_x = cur_x;
			prev_y = cur_y;
		}
	}

	/**
	 * Draws the rest of the data on screen.
	 * 
	 * @param canvas
	 * @param mInterfacePaint
	 */
	private void drawInterface(Canvas canvas, Paint mInterfacePaint) {

		float height = canvas.getHeight();
		float prev_x = -1;
		float prev_y = -1;
		float cur_x;
		float cur_y;

		// If using step function in history mode, use these values.
		// Otherwise create static variables for the current maximum and
		// minimum EDAs to avoid rescaling while in history mode
		if (Global.getClicked()) {
			minEDA = 0;
			maxEDA = 5;
		}

		double[] signal = EDA_history;

		if (!Global.getClicked()) {
			signal = EDA;
		}

		// Draw history. A point every EDA.length / (canvas.width - 180) pixels
		for (int i = 0; i < signal.length - 1; i = i + 15) {
			cur_x = canvas.getWidth()
					- scale_num(i, 0, signal.length - 1, 1,
							canvas.getWidth() - 180);
			cur_y = canvas.getHeight()
					- scale_num((float) signal[i], minEDA - 0.005f,
							maxEDA + 0.005f, 0.9f * height, height - 2);

			if (prev_x != -1) {
				canvas.drawLine(prev_x, prev_y, cur_x, cur_y, mPaint);
			}
			prev_x = cur_x;
			prev_y = cur_y;
		}

		// Draw screen simulation ("you are here" while not on history mode)
		float squareWidth = signal.length / (SR * getTimeMode());
		squareWidth = (canvas.getWidth() - 180) / squareWidth;
		float squareHeight = canvas.getHeight() / 8f;
		float c = -4;
		int counter = 0;

		canvas.drawLine(canvas.getWidth() - 2, 2, canvas.getWidth() - 2,
				squareHeight - c, mInterfacePaint);
		canvas.drawLine(canvas.getWidth() - 2, squareHeight - c,
				canvas.getWidth() - squareWidth, squareHeight - c,
				mInterfacePaint);
		canvas.drawLine(canvas.getWidth() - squareWidth, squareHeight - c,
				canvas.getWidth() - squareWidth, 2, mInterfacePaint);
		canvas.drawLine(canvas.getWidth() - squareWidth, 2,
				canvas.getWidth() - 2, 2, mInterfacePaint);

		// Draw scale for history plot. TODO: it may be a little off.
		mInterfacePaint.setColor(Color.CYAN);
		for (int i = 180; i < canvas.getWidth(); i = (int) (i + squareWidth)) {
			canvas.drawLine(i, squareHeight - c, i, squareHeight - (c + 3),
					mInterfacePaint);
			if (mod(i, 6) == 0) {
				canvas.drawLine(i, squareHeight - c, i, squareHeight - (c - 7),
						mInterfacePaint);
				counter++;
				if (mod(counter, 5) == 0 && counter != 0) {
					canvas.drawLine(i, squareHeight - c, i, squareHeight
							- (c - 8), textPaint);
					canvas.drawText(String.valueOf(15 - counter) + " mins ago",
							i - 127, squareHeight - (c - 25), textPaint);
				}
			}
		}
		counter = 0;
	}

	/**
	 * Draws box, arrows, and feedback text on screen.
	 * 
	 * @param canvas
	 * @param kStart
	 *            : current onset (+ xoom)
	 * @param kEnd
	 *            : current offset (+ xoom)
	 * @param ctrl
	 *            : current zoom
	 * @param paint
	 */

	private void drawMobileBox(Canvas canvas, float kStart, float kEnd,
			float ctrl, Paint paint) {

		float squareHeight = (canvas.getHeight() / 8f) - 4;
		
		
		left = scale_num(kStart - ctrl, 0, EDA_history.length,
				canvas.getWidth(), 180);
		right = scale_num(kEnd + ctrl, 0, EDA_history.length,
				canvas.getWidth(), 180);

		canvas.drawLine(left, 2, left, squareHeight, paint);
		canvas.drawLine(left, squareHeight, right, squareHeight, paint);
		canvas.drawLine(right, squareHeight, right, 2, paint);
		canvas.drawLine(right, 2, left, 2, paint);

		// Draw text
		if (xoom == 0) {
			canvas.drawText("Present", 35, canvas.getHeight() - 5, textPaint);
		} else {
			canvas.drawText(String.valueOf((int) (xoom * (2f / 10)))
					+ " seconds ago.", 35, canvas.getHeight() - 5, textPaint);
		}
		canvas.drawText(
				"Displaying "
						+ String.valueOf((int) Math.abs((80 + zoom) / 240))
						+ " minutes.", canvas.getWidth() - 140,
				canvas.getHeight() - 5, textPaint);

		// Draw arrows
		drawArrow(xoom, zoom, canvas, paint);
	}

	/**
	 * Update horizontal and vertical zooms (zoom and xoom) based on current
	 * position. Limits update of variables to either zoom or xoom, but not both
	 * at the same time.
	 * 
	 * @param k
	 *            : current onset
	 * @param kend
	 *            : current offset
	 * @param hdifference
	 *            : current distance (offset) from relative zero (horizontal)
	 * @param difference
	 *            : current distance (offset) from relative zero (vertical)
	 */
	private void updateXoomZoom(int k, int kend, float hdifference,
			float difference) {

		// Rate of update variables
		int vconstant = 10;
		int constant = processConstant(10, zoom);
		// Sensitivity to motion
		float h = 9;
		float v = 9;

		if (kend + zoom + xoom == EDA_history.length - 1
				&& k <= kend + (zoom + xoom) - min_bin_size) {

			// Update vertical
			if (Math.abs(difference) > v) {
				if (difference > v) {
					zoom = zoom + vconstant;
					if (zoom > 4800) {
						zoom = 4800;
					}
					if (zoom < -40) {
						zoom = -40;
					}
				}
				if (difference < -v) {
					zoom = zoom - vconstant;
					if (zoom > 4800) {
						zoom = 4800;
					}
					if (zoom < -40) {
						zoom = -40;
					}
				}
				return;
			}

			// Update horizontal
			if (hdifference >= h - 2) {
				if (xoom <= temp_xoom) {
					xoom = xoom - constant;
				}
			}
			if (xoom <= 0) {
				xoom = 0;
			}

		}
		if (k - zoom - xoom == 0 && kend + zoom + xoom >= min_bin_size) {

			// Update vertical
			if (Math.abs(difference) > v) {
				if (difference > v) {
					zoom = zoom + vconstant;
					if (zoom > 4800) {
						zoom = 4800;
					}
					if (zoom < -40) {
						zoom = -40;
					}
				}
				if (difference < -v) {
					zoom = zoom - vconstant;
					if (zoom > 4800) {
						zoom = 4800;
					}
					if (zoom < -40) {
						zoom = -40;
					}
				}
				return;
			}
			// Update horizontal, provided that it is within the range of
			// allowed
			// values.
			if (hdifference <= -h + 2) {
				if (xoom <= temp_xoom && xoom < 4800) {
					xoom = xoom + constant;
				}
			}
			if (xoom <= 0) {
				xoom = 0;
			}
		} else {

			// Update vertical
			if (Math.abs(difference) > v) {
				if (difference > v) {
					zoom = zoom + vconstant;
					if (zoom > 4800) {
						zoom = 4800;
					}
					if (zoom < -40) {
						zoom = -40;
					}
				}
				if (difference < -v) {
					zoom = zoom - vconstant;
					if (zoom > 4800) {
						zoom = 4800;
					}
					if (zoom < -40) {
						zoom = -40;
					}
				}
				return;
			}

			// Update horizontal
			if (Math.abs(hdifference) > h) {
				if (hdifference >= h && xoom < 4800) {
					xoom = xoom + constant;
				}
				if (hdifference <= -h + 2 && xoom < 4800) {
					xoom = xoom - constant;
				}
			}
			if (xoom <= 0) {
				xoom = 0;
			}
		}
		return;
	}

	/**
	 * Takes the current state (all values stored in EDA) and clones them into a
	 * new vector for history display. Note that this does not interrupt data
	 * collection while in history mode.
	 */
	public void cloneEDA() {

		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;

		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(); // A
			oos = new ObjectOutputStream(bos);

			// Serialize and pass the object
			oos.writeObject(EDA);
			oos.flush();
			ByteArrayInputStream bin = new ByteArrayInputStream(
					bos.toByteArray());
			ois = new ObjectInputStream(bin);
			// Return the new object and bind EDA_history to it
			EDA_history = (double[]) ois.readObject();
		} catch (Exception e) {
			System.out.println("Exception in ObjectCloner = " + e);
		} finally {
			try {
				oos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				ois.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Draw a step function
		for (int i = 0; i < 4800 - 1; i++) {
			EDA_history[i] = Math.round(((double) i) / 1000);
		}
	}

	/**
	 * Draw the arrows that show up as feedback on head motion
	 * 
	 * @param left
	 *            : amount of horizontal zoom (xoom)
	 * @param up
	 *            : amount of vertical zoom (zoom)
	 * @param canvas
	 * @param paint
	 */
	private void drawArrow(int left, int up, Canvas canvas, Paint paint) {

		mPath.reset();

		// Only draw if zoom and xoom have changed and are not within the
		// limits.
		if (left < temp_xoom) {
			mPath.moveTo(canvas.getWidth() - 30, canvas.getHeight() / 2 - 40);
			mPath.lineTo(canvas.getWidth() - 30, canvas.getHeight() / 2 + 40);
			mPath.lineTo(canvas.getWidth() - 10, canvas.getHeight() / 2);
			mPath.lineTo(canvas.getWidth() - 30, canvas.getHeight() / 2 - 40);
			mPath.close();
			canvas.drawPath(mPath, paint);
			return;
		}
		if (left > temp_xoom) {
			mPath.moveTo(30, canvas.getHeight() / 2 - 40);
			mPath.lineTo(30, canvas.getHeight() / 2 + 40);
			mPath.lineTo(10, canvas.getHeight() / 2);
			mPath.lineTo(30, canvas.getHeight() / 2 - 40);
			mPath.close();
			canvas.drawPath(mPath, paint);
			return;
		}
		if (up > temp_zoom || up == 40) {
			mPath.moveTo(canvas.getWidth() / 2 + 40, 30);
			mPath.lineTo(canvas.getWidth() / 2 - 40, 30);
			mPath.lineTo(canvas.getWidth() / 2, 10);
			mPath.lineTo(canvas.getWidth() / 2 + 40, 30);
			mPath.close();
			canvas.drawPath(mPath, paint);
			return;
		}
		if (up < temp_zoom || up == -40) {
			mPath.moveTo(canvas.getWidth() / 2 + 40, canvas.getHeight() - 30);
			mPath.lineTo(canvas.getWidth() / 2 - 40, canvas.getHeight() - 30);
			mPath.lineTo(canvas.getWidth() / 2, canvas.getHeight() - 10);
			mPath.lineTo(canvas.getWidth() / 2 + 40, canvas.getHeight() - 30);
			mPath.close();
			canvas.drawPath(mPath, paint);
			return;
		}
	}

	// Math methods

	/**
	 * Method that calculates the constant in updateXoomZoom() as a function of
	 * the size of the window.
	 * 
	 * @param def
	 *            : initial value of the constant
	 * @param room
	 *            : amount of zoom in the window
	 * @return : new value of the constant
	 */
	private int processConstant(int def, int room) {

		int constant = (int) (0.2 * (room) + def);

		if (constant <= 0) {
			constant = 3;
		}
		if (constant >= EDA.length / 2) {
			constant = EDA.length / 2;
		}

		return constant;
	}

	/**
	 * Useful function to scale a value @param val in the range [@param min_old, @param
	 * max_old] to a new range [@param min_new, @param max_new]
	 * 
	 * @param val
	 *            : the value to be scaled
	 * @param min_old
	 *            : minimum of original interval
	 * @param max_old
	 *            : maximum of original interval
	 * @param min_new
	 *            : minimum of new interval
	 * @param max_new
	 *            : maximum of new interval
	 * @return : scaled value
	 */
	float scale_num(float val, float min_old, float max_old, float min_new,
			float max_new) {
		if (val < min_old) {
			val = min_old;
		}
		if (val > max_old) {
			val = max_old;
		}
		float valor = ((max_new - min_new) * (val - min_old) / (max_old - min_old))
				+ min_new;
		return valor;
	}

	/**
	 * Reset all variables used in WaveformView.
	 */
	public void reset() {
		Arrays.fill(EDA, 0);
		Arrays.fill(ACL_X, 0);
		Arrays.fill(ACL_Y, 0);
		Arrays.fill(ACL_Z, 0);

		maxEDA = (float) 0.01;
		minEDA = 5;
		maxTEMP = (float) 0.01;
		minTEMP = 99;

		mPath.reset();

		vdifference = 0;
		difference = 0;

		zoom = 0;
		xoom = 0;
		temp_xoom = 0;
		temp_zoom = 0;
		left = 640;
		right = 630.4167f;
	}

	/**
	 * Returns rem(a, b). Useful when dealing with large numbers.
	 * 
	 * @param a
	 * @param b
	 * @return a mod b
	 */
	private static float mod(float a, float b) {
		return (a % b + b) % b;
	}
}