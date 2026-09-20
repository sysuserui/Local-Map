


package com.p.l;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.widget.*;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.location.LocationManager;
import android.provider.Settings;
import android.content.Intent;


public class MainActivity extends AppCompatActivity {
	
	private static final int REQ_GPS = 101;
	
	private GraphView graphView;
	private TextView txtLatLng, txtSpeed, txtDistance, txtTime;
	
	private String currentTrackPath = null;
	
	private final BroadcastReceiver receiver = new BroadcastReceiver() {
	
	
	
	
	
		@Override
		public void onReceive(Context context, Intent intent) {
			
			double lat = intent.getDoubleExtra("lat", 0);
			double lng = intent.getDoubleExtra("lng", 0);
			float speed = intent.getFloatExtra("speed", 0f);
			double totalDist = intent.getDoubleExtra("distance", 0);
			long time = intent.getLongExtra("time", 0);
			
			
			// ⭐ file path (for export)
			if (intent.hasExtra("file"))
			currentTrackPath = intent.getStringExtra("file");
			
			// Graph update (existing compatible)
			graphView.addPoint(lng, lat, speed, 0, totalDist, System.currentTimeMillis());
			
			// UI
			txtLatLng.setText("Lat: " + lat + "  Lng: " + lng);
			txtSpeed.setText(String.format("Speed: %.2f km/h", speed * 3.6f));
			
			if (totalDist < 1000)
			txtDistance.setText(String.format("Distance: %.1f m", totalDist));
			else
			txtDistance.setText(String.format("Distance: %.2f km", totalDist / 1000));
			
			txtTime.setText(formatTime(time));
		}
	};
	
	
	
	
	@Override
	protected void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.activity_main2);
		
		graphView = findViewById(R.id.graphView);
		txtLatLng = findViewById(R.id.txtLatLng);
		txtSpeed = findViewById(R.id.txtSpeed);
		txtDistance = findViewById(R.id.txtDistance);
		txtTime = findViewById(R.id.txtTime);
		
		Button btnExplore = findViewById(R.id.explore);
		Button btnLastTrack = findViewById(R.id.openLastTrack);
		Button btnImage = findViewById(R.id.saveImage);
		btnImage.setOnClickListener(v-> exportGraph());
		FloatingActionButton fab = findViewById(R.id.fabLocation);
		
		// ✅ Explore (unchanged)
		btnExplore.setOnClickListener(v ->
		startActivity(new Intent(this, TrackFileManagerActivity.class))
		);
		
		// ✅ Focus graph
		fab.setOnClickListener(v -> graphView.focusLast());
		
		btnLastTrack.setOnClickListener(v -> openLatestTrackInSavedMap());
		
		// ✅ Manual Export (safe copy)
	//	btnExport.setOnClickListener(v -> manualExport());
		
		checkPermissionAndStart();
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
	
	
	
	private void openLatestTrackInSavedMap() {
		
		try {
			File dir = new File(getExternalFilesDir(null), "tracks");
			
			if (!dir.exists() || !dir.isDirectory()) {
				Toast.makeText(this, "No tracks folder", Toast.LENGTH_SHORT).show();
				return;
			}
			
			File[] files = dir.listFiles();
			
			if (files == null || files.length == 0) {
				Toast.makeText(this, "No track files", Toast.LENGTH_SHORT).show();
				return;
			}
			
			// 🔥 find latest file
			File latest = files[0];
			
			for (File f : files) {
				if (f.lastModified() > latest.lastModified()) {
					latest = f;
				}
			}
			
			// 🔥 open SavedMapActivity with latest file
			Intent i = new Intent(this, SavedMapActivity.class);
			i.putExtra("trackFile", latest.getAbsolutePath());
			startActivity(i);
			
			} catch (Exception e) {
			Toast.makeText(this,
			"Failed to open latest track",
			Toast.LENGTH_LONG).show();
		}
	}
	
	/*
	private void manualExport() {
		
		if (currentTrackPath == null) {
			Toast.makeText(this, "No track yet!", Toast.LENGTH_SHORT).show();
			return;
		}
		
		try {
			File src = new File(currentTrackPath);
			
			File dst = new File(getExternalFilesDir(null),
			"tracks/export_" + System.currentTimeMillis() + ".json");
			
			FileInputStream fis = new FileInputStream(src);
			FileOutputStream fos = new FileOutputStream(dst);
			
			byte[] buf = new byte[1024];
			int len;
			while ((len = fis.read(buf)) > 0) {
				fos.write(buf, 0, len);
			}
			
			fis.close();
			fos.close();
			
			Toast.makeText(this,
			"Exported:\n" + dst.getAbsolutePath(),
			Toast.LENGTH_LONG).show();
			
			} catch (Exception e) {
			Toast.makeText(this,
			"Export failed: " + e.getMessage(),
			Toast.LENGTH_LONG).show();
		}
	}
	
	*/
	
	
	private void checkPermissionAndStart() {
		if (ContextCompat.checkSelfPermission(this,
		Manifest.permission.ACCESS_FINE_LOCATION)
		!= PackageManager.PERMISSION_GRANTED) {
			
			ActivityCompat.requestPermissions(this,
			new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
			REQ_GPS);
			
			} else {
			
			// ✅ Permission OK → ab GPS check
			if (!isLocationEnabled()) {
				Toast.makeText(this,
				"Please turn ON Location",
				Toast.LENGTH_SHORT).show();
				
				openLocationSettings();
				} else {
				startServiceNow();
			}
		}
	}
	
	
	@Override
	public void onRequestPermissionsResult(int code,
	@NonNull String[] perms,
	@NonNull int[] res) {
		
		if (code == REQ_GPS && res.length > 0 &&
		res[0] == PackageManager.PERMISSION_GRANTED) {
			
			// 🔥 ab GPS check
			if (!isLocationEnabled()) {
				openLocationSettings();
				} else {
				startServiceNow();
			}
		}
	}
	
	
	
	private boolean isLocationEnabled() {
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		try {
			return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
			|| lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			} catch (Exception e) {
			return false;
		}
	}
	
	private void openLocationSettings() {
		Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(intent);
	}
	
	
	private void startServiceNow() {
		Intent i = new Intent(this, TrackingService.class);
		ContextCompat.startForegroundService(this, i);
	}

	@Override
	protected void onResume() {
		super.onResume();

		IntentFilter filter =
				new IntentFilter(TrackingService.ACTION_UPDATE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			registerReceiver(
					receiver,
					filter,
					Context.RECEIVER_NOT_EXPORTED
			);
		} else {
			registerReceiver(receiver, filter);
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		try { unregisterReceiver(receiver); } catch (Exception ignored) {}
	}
	
	private String formatTime(long ms) {
		long sec = ms / 1000;
		return String.format("%02d:%02d:%02d",
		sec / 3600,
		(sec % 3600) / 60,
		sec % 60);
	}
	
	
}

