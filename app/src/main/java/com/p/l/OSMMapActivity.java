


package com.p.l;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;

public class OSMMapActivity extends AppCompatActivity {
	
	private MapView map;
	private LocationManager locationManager;
	
	private final int REQ = 100;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// ================= EXTERNAL OFFLINE CACHE SETUP =================
		File basePath = new File(
		Environment.getExternalStoragePublicDirectory(
		Environment.DIRECTORY_DOWNLOADS),
		"OfflineMap/osmdroid"
		);
		
		if (!basePath.exists()) basePath.mkdirs();
		
		Configuration.getInstance().setUserAgentValue(getPackageName());
		Configuration.getInstance().setOsmdroidBasePath(basePath);
		Configuration.getInstance().setOsmdroidTileCache(basePath);
		
		setContentView(R.layout.activity_osm_map);
		
		map = findViewById(R.id.map);
		map.setMultiTouchControls(true);
		
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		if (ActivityCompat.checkSelfPermission(this,
		Manifest.permission.ACCESS_FINE_LOCATION)
		!= PackageManager.PERMISSION_GRANTED) {
			
			ActivityCompat.requestPermissions(this,
			new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ);
			
			} else {
			startLocation();
		}
	}
	
	// ================= LOCATION UPDATES =================
	private void startLocation() {
		
		locationManager.requestLocationUpdates(
		LocationManager.GPS_PROVIDER,
		2000,
		1,
		new LocationListener() {
			
			@Override
			public void onLocationChanged(@NonNull Location location) {
				showOnMap(location);
			}
		}
		);
	}
	
	// ================= MAP UPDATE =================
	private void showOnMap(Location loc) {
		
		GeoPoint point = new GeoPoint(loc.getLatitude(), loc.getLongitude());
		
		map.getController().setZoom(18.0);
		map.getController().setCenter(point);
		
		map.getOverlays().clear();
		
		Marker marker = new Marker(map);
		marker.setPosition(point);
		marker.setTitle("You are here");
		
		map.getOverlays().add(marker);
		
		map.invalidate();
	}
	
	// ================= PERMISSION RESULT =================
	@Override
	public void onRequestPermissionsResult(int requestCode,
	@NonNull String[] permissions,
	@NonNull int[] grantResults) {
		
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		
		if (requestCode == REQ && grantResults.length > 0
		&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			startLocation();
		}
	}
}


