
package com.p.l;

import java.util.ArrayList;
import java.util.List;

public class TrackingRepository {
	
	public static final List<TrackPoint> points = new ArrayList<>();
	public static double totalDistance = 0.0;
	
	// ✅ NEW
	public static int rawPointCount = 0;   // total received points
	public static int validPointCount = 0; // saved points
	public static int sessionId = 0;
	
	public static void clear() {
		points.clear();
		totalDistance = 0.0;
		rawPointCount = 0;
		validPointCount = 0;
	}
}


/*
package com.p.l;

import java.util.ArrayList;
import java.util.List;

public class TrackingRepository {
	
	public static final List<TrackPoint> points = new ArrayList<>();
	public static double totalDistance = 0.0;
	public static float lastSpeed = 0f;
	public static long startTime = 0;
	
	public static void clear() {
		points.clear();
		totalDistance = 0.0;
		lastSpeed = 0f;
		startTime = System.currentTimeMillis();
	}
}

*/