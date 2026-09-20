
package com.p.l;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.widget.ArrayAdapter;
import android.widget.Button;

public class TrackingService extends Service implements LocationListener {
	
	
	
	///
	private int pointCount = 0;
	private static final int IGNORE_FIRST_N = 3;
	
	private long activeTime = 0;
	private long lastUpdateTime = 0;
	private boolean isMoving = false;
	
	public static final String ACTION_UPDATE = "com.p.l.ACTION_UPDATE";
	
	private static final String CHANNEL_ID = "tracking_channel";
	private static final int NOTIF_ID = 101;
	
	String device = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
	
	private LocationManager locationManager;
	private Location lastLocation;
	
	private double totalDistance = 0;
	private long startTime;
	
	private File sessionFile;
	private boolean first = false;
	
	private double speedSum = 0;
	private int speedCount = 0;
	
	private final Gson gson = new Gson();
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		createChannel();
		
		if (intent != null && "STOP".equals(intent.getAction())) {
			stopTracking();
			finalizeFile(); // ✅ proper JSON close
			showSaveNotification(sessionFile.getAbsolutePath());
			stopForeground(true);
			stopSelf();
			return START_NOT_STICKY;
		}
		
		startForeground(NOTIF_ID, buildNotification(0, 0, 0));
		startTracking();
		
		return START_STICKY;
	}
	
	private void startTracking() {
		
		totalDistance = 0;
		startTime = System.currentTimeMillis();
		speedSum = 0;
		speedCount = 0;
		
		sessionFile = new File(getExternalFilesDir(null),
		"tracks/track_" + startTime + ".json");
		
		sessionFile.getParentFile().mkdirs();
		
		// ✅ HEADER (same old structure)
		try {
			FileOutputStream fos = new FileOutputStream(sessionFile);
			String header = "{\n" +
			"  \"startTime\": " + startTime + ",\n" +
			"  \"points\": [\n";
			fos.write(header.getBytes());
			fos.close();
			} catch (Exception e) {
			e.printStackTrace();
		}
		
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
		== PackageManager.PERMISSION_GRANTED) {
			
			locationManager.requestLocationUpdates(
			LocationManager.GPS_PROVIDER,
			2000,
			1,
			this
			);
		}
	}
	
	
	
	/*
	
	@Override
	public void onLocationChanged(Location loc) {
		
		if (pointCount < IGNORE_FIRST_N) {
			pointCount++;
			
			// still update lastLocation so distance jump na ho
			lastLocation = loc;
			
			// reset timing reference safely
			lastUpdateTime = System.currentTimeMillis();
			
			return; // ❌ skip plot + save
		}
		
		
		long now = System.currentTimeMillis();
		
		double segment = 0;
		
		if (lastLocation != null)
		segment = loc.distanceTo(lastLocation);
		
		lastLocation = loc;
		totalDistance += segment;
		
		float speed = loc.hasSpeed() ? loc.getSpeed() : 0f;
		
		speedSum += speed;
		speedCount++;
		
		// ==============================
		// 🟢 ACTIVE TIME LOGIC (NEW)
		// ==============================
		
		final float SPEED_THRESHOLD = 0.5f; // m/s (tune if needed)
		
		if (speed > SPEED_THRESHOLD) {
			
			// user is moving → add time difference
			if (lastUpdateTime != 0) {
				activeTime += (now - lastUpdateTime);
			}
			
			isMoving = true;
			
			} else {
			// user stopped → freeze time
			isMoving = false;
		}
		
		lastUpdateTime = now;
		
		long elapsed = activeTime; // ✅ ONLY MOVEMENT TIME
		
		savePoint(loc, speed, segment, totalDistance);
		sendUpdate(loc, speed, totalDistance, elapsed);
	}
	
	*/
	
	
	/*
	@Override
	public void onLocationChanged(Location loc) {
		
		if (pointCount < IGNORE_FIRST_N) {
			pointCount++;
			
			// still update lastLocation so distance jump na ho
			lastLocation = loc;
			
			return; // ❌ skip plot + save
		}
		
		double segment = 0;
		
		if (lastLocation != null)
		segment = loc.distanceTo(lastLocation);
		
		lastLocation = loc;
		totalDistance += segment;
		
		float speed = loc.hasSpeed() ? loc.getSpeed() : 0f;
		
		speedSum += speed;
		speedCount++;
		
		long elapsed = System.currentTimeMillis() - startTime;
		
		savePoint(loc, speed, segment, totalDistance);
		sendUpdate(loc, speed, totalDistance, elapsed);
	}
	
	*/
	
	
	
	
	
	@Override
	public void onLocationChanged(Location loc) {
		
		
		
		
		double segment = 0;
		
		if (lastLocation != null)
		segment = loc.distanceTo(lastLocation);
		
		lastLocation = loc;
		totalDistance += segment;
		
		float speed = loc.hasSpeed() ? loc.getSpeed() : 0f;
		
		speedSum += speed;
		speedCount++;
		
		long elapsed = System.currentTimeMillis() - startTime;
		
		savePoint(loc, speed, segment, totalDistance);
		sendUpdate(loc, speed, totalDistance, elapsed);
	}
	
	
	
	
	
	private void savePoint(Location loc, float speed, double seg, double total) {
		try {
			FileOutputStream fos = new FileOutputStream(sessionFile, true);
			
			String json = gson.toJson(new TrackPoint(
			loc.getLatitude(),
			loc.getLongitude(),
			speed,
			System.currentTimeMillis(),
			seg,
			total
			));
			
			if (first) json = ",\n  " + json;
			first = true;
			
			fos.write(json.getBytes());
			fos.close();
			
			} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void stopTracking() {
		if (locationManager != null)
		locationManager.removeUpdates(this);
	}
	
	// ✅ FINAL JSON CLOSE (same old format)
	private void finalizeFile() {
		try {
			FileOutputStream fos = new FileOutputStream(sessionFile, true);
			
			double avgSpeed = speedCount > 0
			? (speedSum / speedCount) * 3.6
			: 0;
			
			String footer = "\n  ],\n" +
			"  \"endTime\": " + System.currentTimeMillis() + ",\n" +
			"  \"totalDistance\": " + totalDistance + ",\n" +
			"  \"avgSpeed\": " + avgSpeed + "\n" +
			"}";
			
			fos.write(footer.getBytes());
			fos.close();
			
			} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void sendUpdate(Location loc, float speed, double dist, long time) {
		
		Intent i = new Intent(ACTION_UPDATE);
		i.putExtra("lat", loc.getLatitude());
		i.putExtra("lng", loc.getLongitude());
		i.putExtra("speed", speed);
		i.putExtra("distance", dist);
		i.putExtra("time", time);
		i.putExtra("file", sessionFile.getAbsolutePath());
		
		sendBroadcast(i);
		
		NotificationManager nm =
		(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		if (nm != null)
		nm.notify(NOTIF_ID, buildNotification(speed, dist, time));
	}
	
	private Notification buildNotification(float speed, double dist, long time) {
		
		Intent openIntent = new Intent(this, MainActivity.class);
		openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		PendingIntent openPI = PendingIntent.getActivity(
		this, 0, openIntent,
		PendingIntent.FLAG_IMMUTABLE
		);
		
		Intent stopIntent = new Intent(this, TrackingService.class);
		stopIntent.setAction("STOP");
		
		PendingIntent stopPI = PendingIntent.getService(
		this, 1, stopIntent,
		PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
		);
		
		String t = formatTime(time);
		
		return new NotificationCompat.Builder(this, CHANNEL_ID)
		.setContentTitle("Tracking Running")
		.setContentText(
		"Speed: " + (speed * 3.6f) + " km/h | " +
		"Dist: " + (dist < 1000 ? (int) dist + "m"
		: String.format("%.2f km", dist / 1000)) +
		" | Time: " + t
		)
		.setSmallIcon(android.R.drawable.ic_menu_mylocation)
		.setContentIntent(openPI)
		.addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", stopPI)
		.setOngoing(true)
		.build();
	}
	
	private String formatTime(long ms) {
		long s = ms / 1000;
		return String.format("%02d:%02d:%02d",
		s / 3600,
		(s % 3600) / 60,
		s % 60);
	}
	
	private void createChannel() {
		if (Build.VERSION.SDK_INT >= 26) {
			NotificationChannel ch = new NotificationChannel(
			CHANNEL_ID,
			"Tracking",
			NotificationManager.IMPORTANCE_LOW
			);
			getSystemService(NotificationManager.class)
			.createNotificationChannel(ch);
		}
	}
	
	private void showSaveNotification(String path) {
		
		Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
		.setContentTitle("Track Saved")
		.setContentText("Saved to your device (" + device + ")")
		.setSmallIcon(android.R.drawable.ic_menu_save)
		.setAutoCancel(true)
		.build();
		
		NotificationManager nm =
		(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		if (nm != null)
		nm.notify(NOTIF_ID + 1, notification);
	}
	
	@Override
	public IBinder onBind(Intent intent) { return null; }
}



/*

package com.p.l;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;

public class TrackingService extends Service implements LocationListener {
	
	public static final String ACTION_UPDATE = "com.p.l.ACTION_UPDATE";
	
	private static final String CHANNEL_ID = "tracking_channel";
	private static final int NOTIF_ID = 101;
	
	// ✅ device name (save notification ke liye)
	String device = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
	
	private LocationManager locationManager;
	private Location lastLocation;
	
	private double totalDistance = 0;
	private long startTime;
	
	private File sessionFile;
	private boolean first = false;
	
	private final Gson gson = new Gson();
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		createChannel();
		
		// ✅ STOP from notification
		if (intent != null && "STOP".equals(intent.getAction())) {
			stopTracking();
			finalizeFile(); // ⭐ close JSON
			showSaveNotification(sessionFile.getAbsolutePath()); // ⭐ notify
			stopForeground(true);
			stopSelf();
			return START_NOT_STICKY;
		}
		
		startForeground(NOTIF_ID, buildNotification(0, 0, 0));
		startTracking();
		
		return START_STICKY;
	}
	
	private void startTracking() {
		
		totalDistance = 0;
		startTime = System.currentTimeMillis();
		
		sessionFile = new File(getExternalFilesDir(null),
		"tracks/track_" + startTime + ".json");
		
		sessionFile.getParentFile().mkdirs();
		
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
		== PackageManager.PERMISSION_GRANTED) {
			
			locationManager.requestLocationUpdates(
			LocationManager.GPS_PROVIDER,
			2000,
			1,
			this
			);
		}
	}
	
	@Override
	public void onLocationChanged(Location loc) {
		
		double segment = 0;
		
		if (lastLocation != null)
		segment = loc.distanceTo(lastLocation);
		
		lastLocation = loc;
		totalDistance += segment;
		
		float speed = loc.hasSpeed() ? loc.getSpeed() : 0f;
		long elapsed = System.currentTimeMillis() - startTime;
		
		savePoint(loc, speed, segment, totalDistance);
		sendUpdate(loc, speed, totalDistance, elapsed);
	}
	
	private void savePoint(Location loc, float speed, double seg, double total) {
		try {
			FileOutputStream fos = new FileOutputStream(sessionFile, true);
			
			String json = gson.toJson(new TrackPoint(
			loc.getLatitude(),
			loc.getLongitude(),
			speed,
			System.currentTimeMillis(),
			seg,
			total
			));
			
			if (first) json = ",\n" + json;
			first = true;
			
			fos.write(json.getBytes());
			fos.close();
			
			} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void stopTracking() {
		if (locationManager != null)
		locationManager.removeUpdates(this);
	}
	
	// ✅ JSON close
	private void finalizeFile() {
		try {
			FileOutputStream fos = new FileOutputStream(sessionFile, true);
			fos.write("\n]".getBytes());
			fos.close();
			} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void sendUpdate(Location loc, float speed, double dist, long time) {
		
		Intent i = new Intent(ACTION_UPDATE);
		i.putExtra("lat", loc.getLatitude());
		i.putExtra("lng", loc.getLongitude());
		i.putExtra("speed", speed);
		i.putExtra("distance", dist);
		i.putExtra("time", time);
		
		// ⭐ export ke liye
		i.putExtra("file", sessionFile.getAbsolutePath());
		
		sendBroadcast(i);
		
		NotificationManager nm =
		(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		if (nm != null)
		nm.notify(NOTIF_ID, buildNotification(speed, dist, time));
	}
	
	private Notification buildNotification(float speed, double dist, long time) {
		
		Intent openIntent = new Intent(this, MainActivity.class);
		openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		PendingIntent openPI = PendingIntent.getActivity(
		this, 0, openIntent,
		PendingIntent.FLAG_IMMUTABLE
		);
		
		// ✅ STOP button
		Intent stopIntent = new Intent(this, TrackingService.class);
		stopIntent.setAction("STOP");
		
		PendingIntent stopPI = PendingIntent.getService(
		this, 1, stopIntent,
		PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
		);
		
		String t = formatTime(time);
		
		return new NotificationCompat.Builder(this, CHANNEL_ID)
		.setContentTitle("Tracking Running")
		.setContentText(
		"Speed: " + (speed * 3.6f) + " km/h | " +
		"Dist: " + (dist < 1000 ? (int) dist + "m"
		: String.format("%.2f km", dist / 1000)) +
		" | Time: " + t
		)
		.setSmallIcon(android.R.drawable.ic_menu_mylocation)
		.setContentIntent(openPI)
		.addAction(
		android.R.drawable.ic_menu_close_clear_cancel,
		"STOP",
		stopPI
		)
		.setOngoing(true)
		.build();
	}
	
	private String formatTime(long ms) {
		long s = ms / 1000;
		return String.format("%02d:%02d:%02d",
		s / 3600,
		(s % 3600) / 60,
		s % 60);
	}
	
	private void createChannel() {
		if (Build.VERSION.SDK_INT >= 26) {
			NotificationChannel ch = new NotificationChannel(
			CHANNEL_ID,
			"Tracking",
			NotificationManager.IMPORTANCE_LOW
			);
			getSystemService(NotificationManager.class)
			.createNotificationChannel(ch);
		}
	}
	
	// ✅ Save notification
	private void showSaveNotification(String path) {
		
		Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
		.setContentTitle("Track Saved")
		.setContentText("Saved to your device (" + device + ")")
		.setSmallIcon(android.R.drawable.ic_menu_save)
		.setAutoCancel(true)
		.build();
		
		NotificationManager nm =
		(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		if (nm != null)
		nm.notify(NOTIF_ID + 1, notification);
	}
	
	@Override
	public IBinder onBind(Intent intent) { return null; }
}







*/






/*

package com.p.l;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class TrackingService extends Service implements LocationListener {
	
	public static final String ACTION_UPDATE = "com.p.l.ACTION_UPDATE";
	
	String device = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
	
	private static final String CHANNEL_ID = "tracking_channel";
	private static final int NOTIF_ID = 101;
	
	private LocationManager locationManager;
	private Location lastLocation;
	
	private TrackData trackData;
	private File sessionFile;
	
	private long trackStartTime;
	private boolean isFirstPointSaved = false;
	
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		createChannel();
		
		if (intent != null && "STOP".equals(intent.getAction())) {
			stopTracking();
			stopForeground(true);
			stopSelf();
			return START_NOT_STICKY;
		}
		
		startForeground(NOTIF_ID, buildNotification(0f, 0.0));
		startTracking();
		
		return START_STICKY;
	}
	
	private void startTracking() {
		
		TrackingRepository.rawPointCount = 0;
		TrackingRepository.totalDistance = 0;
		
		trackStartTime = System.currentTimeMillis();
		
		sessionFile = new File(
		getExternalFilesDir(null),
		"tracks/track_" + System.currentTimeMillis() + ".json"
		);
		
		trackData = new TrackData();
		trackData.points = new ArrayList<>();
		trackData.startTime = trackStartTime;
		
		File parentDir = sessionFile.getParentFile();
		if (!parentDir.exists()) parentDir.mkdirs();
		
		try {
			FileOutputStream fos = new FileOutputStream(sessionFile);
			fos.write(("{\n  \"startTime\": " + trackStartTime +
			",\n  \"points\": [\n").getBytes());
			fos.close();
			} catch (Exception e) {
			e.printStackTrace();
		}
		
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
		== PackageManager.PERMISSION_GRANTED) {
			
			locationManager.requestLocationUpdates(
			LocationManager.GPS_PROVIDER,
			2000,
			1,
			this
			);
		}
	}
	
	@Override
	public void onLocationChanged(Location loc) {
		
		TrackingRepository.rawPointCount++;
		
		if (TrackingRepository.rawPointCount <= 5) return;
		
		double segment = 0;
		
		if (lastLocation != null) {
			segment = loc.distanceTo(lastLocation);
		}
		
		lastLocation = loc;
		TrackingRepository.totalDistance += segment;
		
		TrackPoint tp = new TrackPoint();
		tp.lat = loc.getLatitude();
		tp.lng = loc.getLongitude();
		tp.speed = loc.hasSpeed() ? loc.getSpeed() : 0f;
		tp.time = System.currentTimeMillis();
		tp.segmentDistance = segment;
		tp.totalDistance = TrackingRepository.totalDistance;
		
		trackData.points.add(tp);
		
		sendUpdate(tp);
		appendPointToFile(tp);
	}
	
	private void appendPointToFile(TrackPoint point) {
		try {
			FileOutputStream fos = new FileOutputStream(sessionFile, true);
			
			String json = gson.toJson(point);
			
			if (!isFirstPointSaved) {
				isFirstPointSaved = true;
				} else {
				json = ",\n  " + json;
			}
			
			fos.write(json.getBytes());
			fos.close();
			
			} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void stopTracking() {
		try {
			if (locationManager != null) {
				locationManager.removeUpdates(this);
			}
			finalizeJsonFile();
			} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void finalizeJsonFile() {
		try {
			FileOutputStream fos = new FileOutputStream(sessionFile, true);
			
			String footer = String.format(
			"\n  ],\n  \"endTime\": %d,\n  \"totalDistance\": %f,\n  \"avgSpeed\": %f\n}",
			System.currentTimeMillis(),
			TrackingRepository.totalDistance,
			calculateAvgSpeed()
			);
			
			fos.write(footer.getBytes());
			fos.close();
			showSaveNotification(sessionFile.getAbsolutePath());
			
			
			} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private double calculateAvgSpeed() {
		double sum = 0;
		for (TrackPoint p : trackData.points) sum += p.speed;
		return trackData.points.size() > 0 ? (sum / trackData.points.size()) * 3.6 : 0;
	}
	
	private void sendUpdate(TrackPoint tp) {
		Intent i = new Intent(ACTION_UPDATE);
		i.putExtra("lat", tp.lat);
		i.putExtra("lng", tp.lng);
		i.putExtra("speed", tp.speed);
		i.putExtra("segmentDistance", tp.segmentDistance);
		i.putExtra("distance", tp.totalDistance);
		sendBroadcast(i);
		
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (nm != null) nm.notify(NOTIF_ID, buildNotification(tp.speed, tp.totalDistance));
	}
	
	private Notification buildNotification(float speed, double distance) {
		
		Intent stopIntent = new Intent(this, TrackingService.class);
		stopIntent.setAction("STOP");
		
		PendingIntent stopPI = PendingIntent.getService(
		this, 0, stopIntent,
		PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
		);
		
		return new NotificationCompat.Builder(this, CHANNEL_ID)
		.setContentTitle("Tracking Running")
		.setContentText("Speed: " + (speed * 3.6f) + " km/h")
		.setSmallIcon(android.R.drawable.ic_menu_mylocation)
		.addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", stopPI)
		.setOngoing(true)
		.build();
	}
	
	private void createChannel() {
		if (Build.VERSION.SDK_INT >= 26) {
			NotificationChannel ch = new NotificationChannel(
			CHANNEL_ID, "Tracking", NotificationManager.IMPORTANCE_LOW
			);
			getSystemService(NotificationManager.class).createNotificationChannel(ch);
		}
	}
	
	
	
	
	
	///////
	
	
	// Show save notification
	//	showSaveNotification(sessionFile.getAbsolutePath());
	
	
	
	
	
	private void showSaveNotification(String path) {
		Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
		.setContentTitle("Track Saved")
		
		.setContentText("Saved to your device " +device) //path
		.setSmallIcon(android.R.drawable.ic_menu_save)
		.setAutoCancel(true)
		.build();
		
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (nm != null) {
			nm.notify(NOTIF_ID + 1, notification);
		}
	}
	
	
	
	
	
	@Override public IBinder onBind(Intent intent) { return null; }
}


/*

package com.p.l;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class TrackingService extends Service implements LocationListener {
	
	public static final String ACTION_UPDATE = "com.p.l.ACTION_UPDATE";
	
	private static final String CHANNEL_ID = "tracking_channel";
	private static final int NOTIF_ID = 101;
	
	private LocationManager locationManager;
	private Location lastLocation;
	
	private TrackData trackData;
	private File sessionFile;
	
	private long trackStartTime;
	private boolean isFirstPointSaved = false;
	
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	// ================= START =================
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		createChannel();
		
		if (intent != null && "STOP".equals(intent.getAction())) {
			stopTracking();
			stopForeground(true);
			stopSelf();
			return START_NOT_STICKY;
		}
		
		startForeground(NOTIF_ID, buildNotification(0f, 0));
		
		startTracking();
		
		return START_STICKY;
	}
	
	private void startTracking() {
		
		TrackingRepository.rawPointCount = 0;
		TrackingRepository.totalDistance = 0;
		
		trackStartTime = System.currentTimeMillis();
		
		sessionFile = new File(
		getExternalFilesDir(null),
		"tracks/track_" + trackStartTime + ".json"
		);
		
		trackData = new TrackData();
		trackData.points = new ArrayList<>();
		trackData.startTime = trackStartTime;
		
		File parent = sessionFile.getParentFile();
		if (!parent.exists()) parent.mkdirs();
		
		try {
			FileOutputStream fos = new FileOutputStream(sessionFile);
			fos.write(("{\n  \"startTime\": " + trackStartTime +
			",\n  \"points\": [\n").getBytes());
			fos.close();
			} catch (Exception e) {
			e.printStackTrace();
		}
		
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
		== PackageManager.PERMISSION_GRANTED) {
			
			locationManager.requestLocationUpdates(
			LocationManager.GPS_PROVIDER,
			2000,
			1,
			this
			);
		}
	}
	
	// ================= LOCATION =================
	@Override
	public void onLocationChanged(Location loc) {
		
		TrackingRepository.rawPointCount++;
		
		if (TrackingRepository.rawPointCount <= 5) return;
		
		double segment = 0;
		
		if (lastLocation != null) {
			segment = loc.distanceTo(lastLocation);
		}
		
		lastLocation = loc;
		TrackingRepository.totalDistance += segment;
		
		TrackPoint tp = new TrackPoint();
		tp.lat = loc.getLatitude();
		tp.lng = loc.getLongitude();
		tp.speed = loc.hasSpeed() ? loc.getSpeed() : 0f;
		tp.time = System.currentTimeMillis();
		tp.segmentDistance = segment;
		tp.totalDistance = TrackingRepository.totalDistance;
		
		trackData.points.add(tp);
		
		sendUpdate(tp);
		appendPointToFile(tp);
	}
	
	// ================= FILE WRITE =================
	private void appendPointToFile(TrackPoint point) {
		try {
			FileOutputStream fos = new FileOutputStream(sessionFile, true);
			
			String json = gson.toJson(point);
			
			if (!isFirstPointSaved) {
				isFirstPointSaved = true;
				} else {
				json = ",\n  " + json;
			}
			
			fos.write(json.getBytes());
			fos.close();
			
			} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void stopTracking() {
		try {
			if (locationManager != null) {
				locationManager.removeUpdates(this);
			}
			finalizeJsonFile();
			} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void finalizeJsonFile() {
		try {
			FileOutputStream fos = new FileOutputStream(sessionFile, true);
			
			String footer = String.format(
			"\n  ],\n  \"endTime\": %d,\n  \"totalDistance\": %f,\n  \"avgSpeed\": %f\n}",
			System.currentTimeMillis(),
			TrackingRepository.totalDistance,
			calculateAvgSpeed()
			);
			
			fos.write(footer.getBytes());
			fos.close();
			
			showSaveNotification(sessionFile.getAbsolutePath());
			
			} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private double calculateAvgSpeed() {
		double sum = 0;
		for (TrackPoint p : trackData.points) sum += p.speed;
		return trackData.points.size() > 0
		? (sum / trackData.points.size()) * 3.6
		: 0;
	}
	
	// ================= BROADCAST =================
	private void sendUpdate(TrackPoint tp) {
		
		Intent i = new Intent(ACTION_UPDATE);
		i.putExtra("lat", tp.lat);
		i.putExtra("lng", tp.lng);
		i.putExtra("speed", tp.speed);
		i.putExtra("distance", tp.totalDistance);
		
		sendBroadcast(i);
		
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (nm != null) {
			nm.notify(NOTIF_ID, buildNotification(tp.speed, tp.totalDistance));
		}
	}
	
	// ================= NOTIFICATION =================
	private Notification buildNotification(float speed, double distance) {
		
		long elapsed = System.currentTimeMillis() - trackStartTime;
		
		// STOP button
		Intent stopIntent = new Intent(this, TrackingService.class);
		stopIntent.setAction("STOP");
		
		PendingIntent stopPI = PendingIntent.getService(
		this, 0, stopIntent,
		PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
		);
		
		// OPEN APP button
		Intent openIntent = new Intent(this, MainActivity.class);
		openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		PendingIntent openPI = PendingIntent.getActivity(
		this, 1, openIntent,
		PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
		);
		
		String time = formatTime(elapsed);
		
		return new NotificationCompat.Builder(this, CHANNEL_ID)
		.setContentTitle("Tracking Running")
		.setContentText(
		"Speed: " + String.format("%.2f", speed * 3.6f) +
		" km/h | Dist: " + formatDistance(distance) +
		" | Time: " + time
		)
		.setSmallIcon(android.R.drawable.ic_menu_mylocation)
		.setContentIntent(openPI)
		.addAction(android.R.drawable.ic_menu_view, "OPEN APP", openPI)
		.addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", stopPI)
		.setOngoing(true)
		.build();
	}
	
	private String formatDistance(double d) {
		if (d < 1000) return String.format("%.1f m", d);
		return String.format("%.2f km", d / 1000);
	}
	
	private String formatTime(long ms) {
		long sec = ms / 1000;
		long min = sec / 60;
		long hr = min / 60;
		
		sec %= 60;
		min %= 60;
		
		return String.format("%02d:%02d:%02d", hr, min, sec);
	}
	
	private void createChannel() {
		if (Build.VERSION.SDK_INT >= 26) {
			NotificationChannel ch = new NotificationChannel(
			CHANNEL_ID,
			"Tracking",
			NotificationManager.IMPORTANCE_LOW
			);
			getSystemService(NotificationManager.class).createNotificationChannel(ch);
		}
	}
	
	private void showSaveNotification(String path) {
		Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
		.setContentTitle("Track Saved")
		.setContentText("Saved successfully")
		.setSmallIcon(android.R.drawable.ic_menu_save)
		.setAutoCancel(true)
		.build();
		
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (nm != null) {
			nm.notify(NOTIF_ID + 1, notification);
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}











/*

package com.p.l;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class TrackingService extends Service implements LocationListener {
	
	public static final String ACTION_UPDATE = "com.p.l.ACTION_UPDATE";
	
	private static final String CHANNEL_ID = "tracking_channel";
	private static final int NOTIF_ID = 101;
	

	String device = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
	
	private LocationManager locationManager;
	private Location lastLocation;
	
	private TrackData trackData;
	private File sessionFile;
	
	private int sessionId;
	private long trackStartTime;
	private boolean isFirstPointSaved = false;
	
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		createChannel();
		
		if (intent != null && "STOP".equals(intent.getAction())) {
			stopTracking();
			stopForeground(true);
			stopSelf();
			return START_NOT_STICKY;
		}
		
		startForeground(NOTIF_ID, buildNotification(0f, 0.0));
		startTracking();
		
		return START_STICKY;
	}
	
	private int getNewSessionId() {
		SharedPreferences sp = getSharedPreferences("tracker", MODE_PRIVATE);
		int id = sp.getInt("session_id", 0);
		id++;
		sp.edit().putInt("session_id", id).apply();
		return id;
	}
	
	private void startTracking() {
		TrackingRepository.rawPointCount = 0;
		TrackingRepository.totalDistance = 0;
		isFirstPointSaved = false;
		
		sessionId = getNewSessionId();
		trackStartTime = System.currentTimeMillis();
		
		// Create unique filename with timestamp
		sessionFile = new File(
		getExternalFilesDir(null),
		"tracks/track_" + System.currentTimeMillis() + ".json"
		);
		
		// Initialize trackData
		trackData = new TrackData();
		trackData.points = new ArrayList<>();
		trackData.startTime = trackStartTime;
		
		// Create directory if needed
		File parentDir = sessionFile.getParentFile();
		if (!parentDir.exists()) {
			parentDir.mkdirs();
		}
		
		// Write initial JSON structure (opening bracket)
		try {
			FileOutputStream fos = new FileOutputStream(sessionFile);
			fos.write("{\n  \"startTime\": ".getBytes());
			fos.write(String.valueOf(trackStartTime).getBytes());
			fos.write(",\n  \"endTime\": 0,\n  \"totalDistance\": 0,\n  \"avgSpeed\": 0,\n  \"points\": [\n".getBytes());
			fos.close();
			} catch (Exception e) {
			e.printStackTrace();
		}
		
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
		== PackageManager.PERMISSION_GRANTED) {
			
			locationManager.requestLocationUpdates(
			LocationManager.GPS_PROVIDER,
			2000,
			1,
			this
			);
		}
	}
	
	@Override
	public void onLocationChanged(Location loc) {
		double lat = loc.getLatitude();
		double lng = loc.getLongitude();
		float speed = loc.hasSpeed() ? loc.getSpeed() : 0f;
		long time = System.currentTimeMillis();
		
		double segment = 0;
		
		if (lastLocation != null) {
			segment = loc.distanceTo(lastLocation);
		}
		
		lastLocation = loc;
		TrackingRepository.totalDistance += segment;
		
		// Skip first 5 points (GPS stabilization)
		TrackingRepository.rawPointCount++;
		
		if (TrackingRepository.rawPointCount <= 5) {
			return;
		}
		
		// CREATE POINT
		TrackPoint tp = new TrackPoint();
		tp.lat = lat;
		tp.lng = lng;
		tp.speed = speed;
		tp.time = time;
		tp.segmentDistance = segment;
		tp.totalDistance = TrackingRepository.totalDistance;
		
		trackData.points.add(tp);
		sendUpdate(tp);
		
		// ✅ APPEND EACH POINT TO FILE (REAL-TIME)
		appendPointToFile(tp);
	}
	
	// =========================
	// ✅ APPEND MODE - HAR POINT SAVE HOTA HAI
	// =========================
	private void appendPointToFile(TrackPoint point) {
		try {
			FileOutputStream fos = new FileOutputStream(sessionFile, true); // true = append mode
			
			String pointJson;
			
			if (!isFirstPointSaved) {
				// First point - no comma before
				pointJson = gson.toJson(point);
				isFirstPointSaved = true;
				} else {
				// Subsequent points - add comma before
				pointJson = ",\n  " + gson.toJson(point);
			}
			
			fos.write(pointJson.getBytes());
			fos.close();
			
			} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void stopTracking() {
		try {
			if (locationManager != null) {
				locationManager.removeUpdates(this);
			}
			
			// ✅ Close the JSON array and object properly
			finalizeJsonFile();
			
			} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// =========================
	// ✅ JSON FILE KO COMPLETE KARO
	// =========================
	private void finalizeJsonFile() {
		try {
			// Append closing brackets
			FileOutputStream fos = new FileOutputStream(sessionFile, true);
			String footer = String.format(
			"\n  ],\n  \"endTime\": %d,\n  \"totalDistance\": %f,\n  \"avgSpeed\": %f\n}",
			System.currentTimeMillis(),
			TrackingRepository.totalDistance,
			calculateAvgSpeed()
			);
			fos.write(footer.getBytes());
			fos.close();
			
			// Show save notification
			showSaveNotification(sessionFile.getAbsolutePath());
			
			} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private double calculateAvgSpeed() {
		double speedSum = 0;
		for (TrackPoint p : trackData.points) {
			speedSum += p.speed;
		}
		return trackData.points.size() > 0 ? (speedSum / trackData.points.size()) * 3.6 : 0;
	}
	
	private void showSaveNotification(String path) {
		Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
		.setContentTitle("Track Saved")
		
		.setContentText("Saved to your device " +device) //path
		.setSmallIcon(android.R.drawable.ic_menu_save)
		.setAutoCancel(true)
		.build();
		
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (nm != null) {
			nm.notify(NOTIF_ID + 1, notification);
		}
	}
	
	private void sendUpdate(TrackPoint tp) {
		Intent intent = new Intent(ACTION_UPDATE);
		intent.putExtra("lat", tp.lat);
		intent.putExtra("lng", tp.lng);
		intent.putExtra("speed", tp.speed);
		intent.putExtra("segmentDistance", tp.segmentDistance);
		intent.putExtra("distance", tp.totalDistance);
		sendBroadcast(intent);
		
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (nm != null) {
			nm.notify(NOTIF_ID, buildNotification(tp.speed, tp.totalDistance));
		}
	}
	
	private Notification buildNotification(float speed, double distance) {
		String speedTxt = String.format("Speed: %.1f km/h", speed * 3.6f);
		String distTxt = distance < 1000
		? String.format("Distance: %.0f m", distance)
		: String.format("Distance: %.2f km", distance / 1000);
		
		Intent openIntent = new Intent(this, MainActivity.class);
		PendingIntent openPI = PendingIntent.getActivity(
		this, 1, openIntent,
		PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
		);
		
		Intent stopIntent = new Intent(this, TrackingService.class);
		stopIntent.setAction("STOP");
		PendingIntent stopPI = PendingIntent.getService(
		this, 0, stopIntent,
		PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
		);
		
		return new NotificationCompat.Builder(this, CHANNEL_ID)
		.setContentTitle("GPS Tracking Running")
		.setContentText(speedTxt + " | " + distTxt)
		.setSmallIcon(android.R.drawable.ic_menu_mylocation)
		.setOngoing(true)
		.addAction(android.R.drawable.ic_menu_view, "OPEN", openPI)
		.addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", stopPI)
		.build();
	}
	
	private void createChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(
			CHANNEL_ID,
			"Tracking Service",
			NotificationManager.IMPORTANCE_LOW
			);
			NotificationManager nm = getSystemService(NotificationManager.class);
			if (nm != null) nm.createNotificationChannel(channel);
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onProviderEnabled(String provider) {}
	
	@Override
	public void onProviderDisabled(String provider) {}
}



*/

/*

package com.p.l;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class TrackingService extends Service implements LocationListener {
	
	public static final String ACTION_UPDATE = "com.p.l.ACTION_UPDATE";
	
	private static final String CHANNEL_ID = "tracking_channel";
	private static final int NOTIF_ID = 101;
	
	private LocationManager locationManager;
	private Location lastLocation;
	
	private TrackData trackData;
	private File sessionFile;
	
	private int sessionId;
	
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	// =========================
	// START SERVICE
	// =========================
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		createChannel();
		
		if (intent != null && "STOP".equals(intent.getAction())) {
			stopTracking();
			stopForeground(true);
			stopSelf();
			return START_NOT_STICKY;
		}
		
		startForeground(NOTIF_ID, buildNotification(0f, 0.0));
		
		startTracking();
		
		return START_STICKY;
	}
	
	// =========================
	// SESSION
	// =========================
	private int getNewSessionId() {
		SharedPreferences sp = getSharedPreferences("tracker", MODE_PRIVATE);
		int id = sp.getInt("session_id", 0);
		id++;
		sp.edit().putInt("session_id", id).apply();
		return id;
	}
	
	// =========================
	// START TRACKING
	// =========================
	private void startTracking() {
		
		// RESET COUNTER ONLY ONCE PER SESSION START
		TrackingRepository.rawPointCount = 0;
		
		sessionId = getNewSessionId();
		
		sessionFile = new File(
		getExternalFilesDir(null),
		"tracks/session" + sessionId + ".json"
		);
		
		trackData = new TrackData();
		trackData.points = new ArrayList<>();
		
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
		== PackageManager.PERMISSION_GRANTED) {
			
			locationManager.requestLocationUpdates(
			LocationManager.GPS_PROVIDER,
			2000,
			1,
			this
			);
		}
	}
	
	// =========================
	// LOCATION UPDATE
	// =========================
	@Override
	public void onLocationChanged(Location loc) {
		
		double lat = loc.getLatitude();
		double lng = loc.getLongitude();
		float speed = loc.hasSpeed() ? loc.getSpeed() : 0f;
		long time = System.currentTimeMillis();
		
		double segment = 0;
		
		if (lastLocation != null) {
			segment = loc.distanceTo(lastLocation);
		}
		
		lastLocation = loc;
		
		TrackingRepository.totalDistance += segment;
		
		// =========================
		// FIXED LOGIC: SKIP ONLY FIRST 5 POINTS
		// =========================
		TrackingRepository.rawPointCount++;
		
		if (TrackingRepository.rawPointCount <= 5) {
			return; // only first 5 ignored, after that ALL saved
		}
		
		// =========================
		// CREATE POINT
		// =========================
		TrackPoint tp = new TrackPoint();
		tp.lat = lat;
		tp.lng = lng;
		tp.speed = speed;
		tp.time = time;
		tp.segmentDistance = segment;
		tp.totalDistance = TrackingRepository.totalDistance;
		
		trackData.points.add(tp);
		
		sendUpdate(tp);
		
		// =========================
		// SAVE EVERY VALID POINT
		// =========================
		autoSave();
	}
	
	// =========================
	// AUTO SAVE (STABLE)
	// =========================
	private void autoSave() {
		
		try {
			trackData.totalDistance = TrackingRepository.totalDistance;
			trackData.endTime = System.currentTimeMillis();
			
			String json = gson.toJson(trackData);
			
			FileOutputStream fos = new FileOutputStream(sessionFile);
			fos.write(json.getBytes());
			fos.close();
			
			} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// =========================
	// STOP TRACKING
	// =========================
	private void stopTracking() {
		try {
			if (locationManager != null) {
				locationManager.removeUpdates(this);
			}
			
			autoSave();
			
			} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// =========================
	// UPDATE UI
	// =========================
	private void sendUpdate(TrackPoint tp) {
		
		Intent intent = new Intent(ACTION_UPDATE);
		intent.putExtra("lat", tp.lat);
		intent.putExtra("lng", tp.lng);
		intent.putExtra("speed", tp.speed);
		intent.putExtra("segmentDistance", tp.segmentDistance);
		intent.putExtra("distance", tp.totalDistance);
		
		sendBroadcast(intent);
		
		NotificationManager nm =
		(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		if (nm != null) {
			nm.notify(NOTIF_ID, buildNotification(tp.speed, tp.totalDistance));
		}
	}
	
	// =========================
	// NOTIFICATION
	// =========================
	private Notification buildNotification(float speed, double distance) {
		
		String speedTxt = String.format("Speed: %.1f km/h", speed * 3.6f);
		
		String distTxt = distance < 1000
		? String.format("Distance: %.0f m", distance)
		: String.format("Distance: %.2f km", distance / 1000);
		
		Intent openIntent = new Intent(this, MainActivity.class);
		
		PendingIntent openPI = PendingIntent.getActivity(
		this, 1, openIntent,
		PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
		);
		
		Intent stopIntent = new Intent(this, TrackingService.class);
		stopIntent.setAction("STOP");
		
		PendingIntent stopPI = PendingIntent.getService(
		this, 0, stopIntent,
		PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
		);
		
		return new NotificationCompat.Builder(this, CHANNEL_ID)
		.setContentTitle("GPS Tracking Running")
		.setContentText(speedTxt + " | " + distTxt)
		.setSmallIcon(android.R.drawable.ic_menu_mylocation)
		.setOngoing(true)
		.addAction(android.R.drawable.ic_menu_view, "OPEN", openPI)
		.addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", stopPI)
		.build();
	}
	
	// =========================
	// CHANNEL
	// =========================
	private void createChannel() {
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			
			NotificationChannel channel = new NotificationChannel(
			CHANNEL_ID,
			"Tracking Service",
			NotificationManager.IMPORTANCE_LOW
			);
			
			NotificationManager nm = getSystemService(NotificationManager.class);
			if (nm != null) nm.createNotificationChannel(channel);
		}
	}
	
	@Override public IBinder onBind(Intent intent) { return null; }
	@Override public void onProviderEnabled(String provider) {}
	@Override public void onProviderDisabled(String provider) {}
}




*/



/*
package com.p.l;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class TrackingService extends Service implements LocationListener {
	
	public static final String ACTION_UPDATE = "com.p.l.ACTION_UPDATE";
	
	private static final String CHANNEL_ID = "tracking_channel";
	private static final int NOTIF_ID = 101;
	
	private LocationManager locationManager;
	private Location lastLocation;
	
	private TrackData trackData;
	private File sessionFile;
	
	// =========================
	// START SERVICE
	// =========================
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		createChannel();
		
		// =========================
		// STOP BUTTON HANDLER
		// =========================
		if (intent != null && "STOP".equals(intent.getAction())) {
			
			stopTracking();   // cleanup
			stopForeground(true);
			stopSelf();
			
			return START_NOT_STICKY;
		}
		
		startForeground(NOTIF_ID, buildNotification(0f, 0.0));
		
		startTracking();
		
		return START_STICKY;
	}
	
	
	
	
	
	// =========================
	// INIT TRACKING SESSION
	// =========================
	private void startTracking() {
		
		TrackingRepository.clear();
		TrackingRepository.sessionId++;
		
		trackData = new TrackData();
		trackData.points = new ArrayList<>();
		
		sessionFile = new File(
		getExternalFilesDir(null),
		"tracks/session_" + TrackingRepository.sessionId + ".json"
		);
		
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
		== PackageManager.PERMISSION_GRANTED) {
			
			locationManager.requestLocationUpdates(
			LocationManager.GPS_PROVIDER,
			2000,
			1,
			this
			);
		}
	}
	
	// =========================
	// LOCATION UPDATE
	// =========================
	@Override
	public void onLocationChanged(Location loc) {
		
		TrackingRepository.rawPointCount++;
		
		double lat = loc.getLatitude();
		double lng = loc.getLongitude();
		float speed = loc.hasSpeed() ? loc.getSpeed() : 0f;
		
		long time = System.currentTimeMillis();
		
		double segment = 0;
		
		if (lastLocation != null) {
			segment = loc.distanceTo(lastLocation);
		}
		
		lastLocation = loc;
		
		TrackingRepository.totalDistance += segment;
		
		// ❌ IGNORE FIRST 5 POINTS (unstable GPS)
		if (TrackingRepository.rawPointCount <= 5) {
			return;
		}
		
		TrackPoint tp = new TrackPoint();
		tp.lat = lat;
		tp.lng = lng;
		tp.speed = speed;
		tp.time = time;
		tp.segmentDistance = segment;
		tp.totalDistance = TrackingRepository.totalDistance;
		
		// =========================
		// ADD TO SINGLE SESSION
		// =========================
		trackData.points.add(tp);
		
		sendUpdate(tp);
		
		// =========================
		// APPEND SAVE (ONE FILE ONLY)
		// =========================
		appendToFile();
	}
	
	// =========================
	// APPEND SAVE FUNCTION
	// =========================
	private void appendToFile() {
		try {
			trackData.totalDistance = TrackingRepository.totalDistance;
			trackData.endTime = System.currentTimeMillis();
			
			FileOutputStream fos = new FileOutputStream(sessionFile);
			String json = new com.google.gson.GsonBuilder()
			.setPrettyPrinting()
			.create()
			.toJson(trackData);
			
			fos.write(json.getBytes());
			fos.close();
			
			} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	private void stopTracking() {

    try {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }

        // optional reset
        TrackingRepository.clear();

    } catch (Exception e) {
        e.printStackTrace();
    }
}

	// =========================
	// UPDATE UI
	// =========================
	private void sendUpdate(TrackPoint tp) {
		
		Intent intent = new Intent(ACTION_UPDATE);
		intent.putExtra("lat", tp.lat);
		intent.putExtra("lng", tp.lng);
		intent.putExtra("speed", tp.speed);
		intent.putExtra("segmentDistance", tp.segmentDistance);
		intent.putExtra("distance", tp.totalDistance);
		
		sendBroadcast(intent);
		
		// 🔔 UPDATE NOTIFICATION ALSO
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (nm != null) {
			nm.notify(NOTIF_ID, buildNotification(tp.speed, tp.totalDistance));
		}
	}
	
	// =========================
	// NOTIFICATION (UPGRADED LIKE YOUR SECOND VERSION)
	// =========================
	private Notification buildNotification(float speed, double distance) {
		
		String speedTxt = String.format("Speed: %.1f km/h", speed * 3.6f);
		
		String distTxt = distance < 1000
		? String.format("Distance: %.0f m", distance)
		: String.format("Distance: %.2f km", distance / 1000);
		
		Intent openIntent = new Intent(this, MainActivity.class);
		openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		PendingIntent openPI = PendingIntent.getActivity(
		this, 1, openIntent,
		PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
		);
		
		Intent stopIntent = new Intent(this, TrackingService.class);
		stopIntent.setAction("STOP");
		
		PendingIntent stopPI = PendingIntent.getService(
		this, 0, stopIntent,
		PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
		);
		
		return new NotificationCompat.Builder(this, CHANNEL_ID)
		.setContentTitle("GPS Tracking Running")
		.setContentText(speedTxt + " | " + distTxt)
		.setSmallIcon(android.R.drawable.ic_menu_mylocation)
		.setOngoing(true)
		.addAction(android.R.drawable.ic_menu_view, "OPEN", openPI)
		.addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", stopPI)
		.build();
	}
	
	// =========================
	// CHANNEL
	// =========================
	private void createChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			
			NotificationChannel channel = new NotificationChannel(
			CHANNEL_ID,
			"Tracking Service",
			NotificationManager.IMPORTANCE_LOW
			);
			
			NotificationManager nm = getSystemService(NotificationManager.class);
			if (nm != null) nm.createNotificationChannel(channel);
		}
	}
	
	@Override public IBinder onBind(Intent intent) { return null; }
	@Override public void onProviderEnabled(String provider) {}
	@Override public void onProviderDisabled(String provider) {}
}






/*
package com.p.l;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class TrackingService extends Service implements LocationListener {
	
	public static final String ACTION_UPDATE = "com.p.l.ACTION_UPDATE";
	
	private static final String CHANNEL_ID = "tracking_channel";
	private static final int NOTIF_ID = 101;
	
	private LocationManager locationManager;
	private Location lastLocation;
	
	// =========================
	// START SERVICE PROPERLY
	// =========================
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		createChannel();
		
		Notification notification = buildNotification();
		
		// ✅ MUST CALL IMMEDIATELY (fix crash)
		startForeground(NOTIF_ID, notification);
		
		startTracking();
		
		return START_STICKY;
	}
	
	// =========================
	// GPS START LOGIC
	// =========================
	private void startTracking() {
		
		TrackingRepository.clear();
		TrackingRepository.sessionId++;
		
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
		== PackageManager.PERMISSION_GRANTED) {
			
			locationManager.requestLocationUpdates(
			LocationManager.GPS_PROVIDER,
			2000,
			1,
			this
			);
		}
	}
	
	// =========================
	// LOCATION UPDATE
	// =========================
	@Override
	public void onLocationChanged(Location loc) {
		
		TrackingRepository.rawPointCount++;
		
		double lat = loc.getLatitude();
		double lng = loc.getLongitude();
		float speed = loc.hasSpeed() ? loc.getSpeed() : 0f;
		
		long time = System.currentTimeMillis();
		
		double segment = 0;
		
		if (lastLocation != null) {
			segment = loc.distanceTo(lastLocation);
		}
		
		lastLocation = loc;
		
		TrackingRepository.totalDistance += segment;
		
		// =====================================================
		// ❌ IGNORE FIRST 5 UNSTABLE GPS POINTS
		// =====================================================
		if (TrackingRepository.rawPointCount <= 5) {
			return;
		}
		
		// =====================================================
		// SAVE STABLE DATA ONLY
		// =====================================================
		TrackPoint tp = new TrackPoint();
		tp.lat = lat;
		tp.lng = lng;
		tp.speed = speed;
		tp.time = time;
		tp.segmentDistance = segment;
		tp.totalDistance = TrackingRepository.totalDistance;
		
		TrackingRepository.points.add(tp);
		TrackingRepository.validPointCount++;
		
		// =====================================================
		// AUTO SAVE EVERY 5 VALID POINTS
		// =====================================================
		if (TrackingRepository.validPointCount % 5 == 0) {
			autoSave();
		}
		
		sendUpdate(tp);
	}
	
	// =========================
	// SEND DATA TO UI
	// =========================
	private void sendUpdate(TrackPoint tp) {
		
		Intent intent = new Intent(ACTION_UPDATE);
		intent.putExtra("lat", tp.lat);
		intent.putExtra("lng", tp.lng);
		intent.putExtra("speed", tp.speed);
		intent.putExtra("segmentDistance", tp.segmentDistance);
		intent.putExtra("distance", tp.totalDistance);
		
		sendBroadcast(intent);
	}
	
	// =========================
	// AUTO SAVE JSON
	// =========================
	private void autoSave() {
		try {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			
			TrackData data = new TrackData();
			data.startTime = System.currentTimeMillis();
			data.endTime = System.currentTimeMillis();
			data.totalDistance = TrackingRepository.totalDistance;
			data.points = new ArrayList<>(TrackingRepository.points);
			
			File dir = new File(getExternalFilesDir(null), "tracks");
			if (!dir.exists()) dir.mkdirs();
			
			File file = new File(
			dir,
			"track_session_" + TrackingRepository.sessionId + "_" + System.currentTimeMillis() + ".json"
			);
			
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(gson.toJson(data).getBytes());
			fos.close();
			
			} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// =========================
	// NOTIFICATION (IMPORTANT FIX)
	// =========================
	private Notification buildNotification() {
		
		createChannel();
		
		return new NotificationCompat.Builder(this, CHANNEL_ID)
		.setContentTitle("GPS Tracking Running")
		.setContentText("Tracking active...")
		.setSmallIcon(android.R.drawable.ic_menu_mylocation)
		.setOngoing(true)
		.build();
	}
	
	// =========================
	// NOTIFICATION CHANNEL
	// =========================
	private void createChannel() {
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			
			NotificationChannel channel = new NotificationChannel(
			CHANNEL_ID,
			"Tracking Service",
			NotificationManager.IMPORTANCE_LOW
			);
			
			NotificationManager manager = getSystemService(NotificationManager.class);
			if (manager != null) {
				manager.createNotificationChannel(channel);
			}
		}
	}
	
	@Override public IBinder onBind(Intent intent) { return null; }
	@Override public void onProviderEnabled(String provider) {}
	@Override public void onProviderDisabled(String provider) {}
}

*/
// upr ka delete




/*

package com.p.l;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.NotificationCompat;

public class TrackingService extends Service implements LocationListener {
	
	public static final String ACTION_UPDATE = "com.p.m.ACTION_UPDATE";
	public static final String ACTION_STOP   = "com.p.m.ACTION_STOP";
	
	private static final String CHANNEL_ID = "tracking_channel";
	private static final int NOTIF_ID = 1;
	
	private LocationManager locationManager;
	private PowerManager.WakeLock wakeLock;
	private Location lastLocation;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		TrackingRepository.clear();
		
		createChannel();
		
		// ✅ WakeLock
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = pm.newWakeLock(
		PowerManager.PARTIAL_WAKE_LOCK,
		"MyApp:TrackingLock"
		);
		wakeLock.setReferenceCounted(false);
		wakeLock.acquire(10 * 60 * 1000L); // 10 min
		
		startForeground(
		NOTIF_ID,
		buildNotification(0f, 0.0)
		);
		
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
		== PackageManager.PERMISSION_GRANTED) {
			
			locationManager.requestLocationUpdates(
			LocationManager.GPS_PROVIDER,
			2000,
			1,
			this
			);
		}
	}
	
	// ================= NOTIFICATION =================
	
	private Notification buildNotification(float speed, double distance) {
		
		String speedTxt = String.format("Speed: %.1f km/h", speed * 3.6f);
		
		String distTxt = distance < 1000
		? String.format("Distance: %.0f m", distance)
		: String.format("Distance: %.2f km", distance / 1000);
		
		Intent openIntent = new Intent(this, MainActivity.class);
		openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		PendingIntent openPI = PendingIntent.getActivity(
		this, 1, openIntent,
		PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
		);
		
		Intent stopIntent = new Intent(this, TrackingService.class);
		stopIntent.setAction(ACTION_STOP);
		
		PendingIntent stopPI = PendingIntent.getService(
		this, 0, stopIntent,
		PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
		);
		
		return new NotificationCompat.Builder(this, CHANNEL_ID)
		.setContentTitle("GPS Tracking Running")
		.setContentText(speedTxt + " | " + distTxt)
		.setSmallIcon(android.R.drawable.ic_menu_mylocation)
		.setOngoing(true)
		.addAction(android.R.drawable.ic_menu_view, "OPEN APP", openPI)
		.addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", stopPI)
		.build();
	}
	
	private void createChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel ch = new NotificationChannel(
			CHANNEL_ID, "Tracking",
			NotificationManager.IMPORTANCE_DEFAULT
			);
			NotificationManager nm = getSystemService(NotificationManager.class);
			if (nm != null) nm.createNotificationChannel(ch);
		}
	}
	
	// ================= COMMAND =================
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		if (intent != null && ACTION_STOP.equals(intent.getAction())) {
			stopSelf();
			return START_NOT_STICKY;
		}
		
		return START_NOT_STICKY;
	}
	
	// ================= LOCATION =================
	
	@Override
	public void onLocationChanged(Location loc) {
		
		TrackPoint tp = new TrackPoint();
		tp.lat = loc.getLatitude();
		tp.lng = loc.getLongitude();
		tp.speed = loc.hasSpeed() ? loc.getSpeed() : 0f;
		tp.time = System.currentTimeMillis();
		
		// ✅ segment distance (previous point se)
		if (lastLocation != null) {
			tp.segmentDistance = loc.distanceTo(lastLocation);
			} else {
			tp.segmentDistance = 0;
		}
		
		// ✅ total distance update
		TrackingRepository.totalDistance += tp.segmentDistance;
		tp.totalDistance = TrackingRepository.totalDistance;
		
		lastLocation = loc;
		
		// save in repository
		TrackingRepository.points.add(tp);
		
		// 🔔 notification update
		NotificationManager nm =
		(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		if (nm != null) {
			nm.notify(
			NOTIF_ID,
			buildNotification(tp.speed, tp.totalDistance)
			);
		}
		
		// ✅ SEND DATA TO ACTIVITY
		Intent intent = new Intent(ACTION_UPDATE);
		intent.setPackage(getPackageName());
		
		intent.putExtra("lat", tp.lat);
		intent.putExtra("lng", tp.lng);
		intent.putExtra("speed", tp.speed);
		
		// total distance
		intent.putExtra("distance", tp.totalDistance);
		
		// segment distance
		intent.putExtra("segmentDistance", tp.segmentDistance);
		
		sendBroadcast(intent);
	}
	
	@Override public void onProviderEnabled(String p) {}
	@Override public void onProviderDisabled(String p) {}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (locationManager != null) {
			locationManager.removeUpdates(this);
		}
		
		if (wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}





*/
