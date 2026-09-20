package com.p.l;





public class MainActivity1{
	
}


/*

import android.os.PowerManager;
import android.content.Intent;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Environment;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileOutputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity1 extends AppCompatActivity




implements LocationListener {
	
	
	private TrackData trackData = new TrackData();
	private long trackStartTime;
	// upr
	
	private static final int REQ_GPS = 101;
	private PowerManager.WakeLock wakeLock;
	private GraphView graphView;
	private LocationManager locationManager;
	
	private TextView txtLatLng, txtSpeed, txtDistance;
	
	private Location lastLocation = null;
	private double totalDistance = 0.0; // meters
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		graphView = findViewById(R.id.graphView);
		txtLatLng = findViewById(R.id.txtLatLng);
		txtSpeed = findViewById(R.id.txtSpeed);
		txtDistance = findViewById(R.id.txtDistance);
		
		Button btnOpenMap = findViewById(R.id.openMap);
		btnOpenMap.setOnClickListener(v -> openLastSavedTrack());
		
		FloatingActionButton fab = findViewById(R.id.fabLocation);
		fab.setOnClickListener(v -> graphView.focusLast());
		
		Button btn = findViewById(R.id.export);
		btn.setOnClickListener(v -> finishAndSaveTrack());
		
		Button btnexplore = findViewById(R.id.explore);
		btnexplore.setOnClickListener(v -> see());
		
		locationManager =
		(LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		trackStartTime = System.currentTimeMillis();
		trackData.points.clear();
		
		// 🔐 Permission check
		if (ActivityCompat.checkSelfPermission(
		this,
		Manifest.permission.ACCESS_FINE_LOCATION
		) != PackageManager.PERMISSION_GRANTED) {
			
			ActivityCompat.requestPermissions(
			this,
			new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
			REQ_GPS
			);
			return;
		}
		
		// 📍 Start foreground tracking service (MOST IMPORTANT)
		Intent serviceIntent = new Intent(this, TrackingService.class);
		ContextCompat.startForegroundService(this, serviceIntent);
		
		// 📡 Start GPS for UI updates
		startGPS();
	}
	
	
	
	
	private void startGPS() {
		
		if (ActivityCompat.checkSelfPermission(
		this,
		Manifest.permission.ACCESS_FINE_LOCATION
		) != PackageManager.PERMISSION_GRANTED) {
			return;
		}
		
		if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			Toast.makeText(this,
			"Please enable GPS",
			Toast.LENGTH_LONG).show();
			return;
		}
		
		locationManager.requestLocationUpdates(
		LocationManager.GPS_PROVIDER,
		20,   // 2 seconds
		1,      // 1 meter
		this
		);
	}
	
	//* ================= GPS CALLBACK =================
	
	
	
	@Override
	public void onLocationChanged(@NonNull Location loc) {
		
		double lat = loc.getLatitude();
		double lng = loc.getLongitude();
		
		// ===== TrackPoint create =====
		TrackPoint tp = new TrackPoint();
		tp.lat = lat;
		tp.lng = lng;
		tp.speed = loc.hasSpeed() ? loc.getSpeed() : 0f;
		tp.time = System.currentTimeMillis();
		
		if (lastLocation != null) {
			tp.distanceFromLast = loc.distanceTo(lastLocation);
			totalDistance += tp.distanceFromLast;
			} else {
			tp.distanceFromLast = 0;
		}
		
		trackData.points.add(tp);
		
		
		graphView.addPoint(
		lng,
		lat,
		tp.speed,
		tp.distanceFromLast,   // segment distance
		totalDistance,         // ✅ ADD THIS
		tp.time
		);
		
		
		// ===== GRAPH =====
		
		
		//	graphView.addPoint(lng, lat);
		
		// ===== UI =====
		txtLatLng.setText(
		String.format("Lat: %.6f   Lng: %.6f", lat, lng)
		);
		
		float speedKmh = tp.speed * 3.6f;
		txtSpeed.setText(
		String.format("Speed: %.2f km/h", speedKmh)
		);
		
		if (totalDistance < 1000) {
			txtDistance.setText(
			String.format("Distance: %.2f m", totalDistance)
			);
			} else {
			txtDistance.setText(
			String.format("Distance: %.3f km", totalDistance / 1000)
			);
		}
		
		lastLocation = loc;
	}
	
	
	
	@Override public void onProviderEnabled(@NonNull String p) {}
	@Override public void onProviderDisabled(@NonNull String p) {}
	
	//* ================= PERMISSION RESULT =================
	
	@Override
	public void onRequestPermissionsResult(
	int code,
	@NonNull String[] perms,
	@NonNull int[] res) {
		
		super.onRequestPermissionsResult(code, perms, res);
		
		if (code == REQ_GPS && res.length > 0
		&& res[0] == PackageManager.PERMISSION_GRANTED) {
			startGPS();
			
			
			// 🔥 START BACKGROUND TRACKING SERVICE
			Intent serviceIntent = new Intent(this, TrackingService.class);
			ContextCompat.startForegroundService(this, serviceIntent);
			} else {
			Toast.makeText(this,
			"GPS permission required",
			Toast.LENGTH_LONG).show();
		}
	}
	
	
	
	private void exportGraph() {
		try {
			Bitmap bitmap = graphView.exportBitmap();
			
			File dir = Environment.getExternalStoragePublicDirectory(
			Environment.DIRECTORY_DOWNLOADS
			);
			
			if (!dir.exists()) dir.mkdirs();
			
			File file = new File(dir,
			"graph_" + System.currentTimeMillis() + ".png");
			
			FileOutputStream fos = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
			fos.flush();
			fos.close();
			
			Toast.makeText(this,
			"Graph saved in Downloads",
			Toast.LENGTH_LONG).show();
			
			} catch (Exception e) {
			Toast.makeText(this,
			"Export failed: " + e.getMessage(),
			Toast.LENGTH_SHORT).show();
		}
	}
	
	
	
	
	
	
	private void saveTrackJson() throws Exception {
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(trackData);
		
		File dir = new File(getExternalFilesDir(null), "tracks");
		if (!dir.exists()) dir.mkdirs();
		
		File file = new File(dir,
		"track_" + System.currentTimeMillis() + ".json");
		
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(json.getBytes());
		fos.close();
		
		Toast.makeText(this,
		"Track data saved",
		Toast.LENGTH_LONG).show();
	}
	
	private void finishAndSaveTrack() {
		try {
			trackData.startTime = trackStartTime;
			trackData.endTime = System.currentTimeMillis();
			trackData.totalDistance = totalDistance;
			
			double speedSum = 0;
			for (TrackPoint p : trackData.points) {
				speedSum += p.speed;
			}
			
			trackData.avgSpeed = trackData.points.size() > 0
			? (speedSum / trackData.points.size()) * 3.6
			: 0;
			
			saveTrackJson();
			
			} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void openLastSavedTrack() {
		
		File dir = new File(getExternalFilesDir(null), "tracks");
		if (!dir.exists()) {
			Toast.makeText(this, "No saved tracks", Toast.LENGTH_LONG).show();
			return;
		}
		
		File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
		
		if (files == null || files.length == 0) {
			Toast.makeText(this, "No saved tracks found", Toast.LENGTH_LONG).show();
			return;
		}
		
		// latest file uthao
		File latest = files[0];
		for (File f : files) {
			if (f.lastModified() > latest.lastModified()) {
				latest = f;
			}
		}
		
		
		Intent i = new Intent(this, SavedMapActivity.class);
		i.putExtra("trackFile", latest.getAbsolutePath());
		startActivity(i);
		
		
		
	}
	
	
	private void see(){
		
		startActivity(
		new Intent(this, TrackFileManagerActivity.class)
		);
		
		
	}
	
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
		}
	}
	
	
	
	
	
	
	
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			
			if (TrackingRepository.points.isEmpty()) return;
			
			TrackPoint tp =
			TrackingRepository.points.get(
			TrackingRepository.points.size() - 1
			);
			
			graphView.addPoint(
			tp.lng,
			tp.lat,
			tp.speed,
			tp.distanceFromLast,
			TrackingRepository.totalDistance,
			tp.time
			);
			
			txtSpeed.setText(
			String.format("Speed: %.2f km/h", tp.speed * 3.6f)
			);
			
			if (TrackingRepository.totalDistance < 1000) {
				txtDistance.setText(
				String.format("Distance: %.1f m", TrackingRepository.totalDistance)
				);
				} else {
				txtDistance.setText(
				String.format("Distance: %.2f km",
				TrackingRepository.totalDistance / 1000)
				);
			}
		}
	};
	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// WakeLock
		if (wakeLock != null && !wakeLock.isHeld()) {
			wakeLock.acquire();
		}
		
		registerReceiver(
		receiver,
		new IntentFilter(TrackingService.ACTION_UPDATE)
		);
		
		// 🔁 RE-DRAW ALL POINTS
		graphView.clear();
		for (TrackPoint p : TrackingRepository.points) {
			graphView.addPoint(
			p.lng,
			p.lat,
			p.speed,
			p.distanceFromLast,
			TrackingRepository.totalDistance,
			p.time
			);
		}
	}
	
	
	
	@Override
	protected void onPause() {
		super.onPause();
		
		// WakeLock
		if (wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
		}
		
		// Receiver
		unregisterReceiver(receiver);
	}
	
}


*/