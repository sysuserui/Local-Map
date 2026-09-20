package com.p.l;

import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.File;

public class OfflineRadiusActivity extends AppCompatActivity {
	
	private MapView map;
	private TextView status;
	private Button btnDownload;
	
	private LocationManager locationManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_download1);
		
		map = findViewById(R.id.map);
		status = findViewById(R.id.txtStatus);
		btnDownload = findViewById(R.id.btnDownload);
		
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		setupExternalCache();
		
		map.setMultiTouchControls(true);
		map.setTileSource(TileSourceFactory.MAPNIK);
		
		btnDownload.setOnClickListener(v -> startDownload());
	}
	
	// ================= EXTERNAL STORAGE SET =================
	private void setupExternalCache() {
		
		File base = new File(
		Environment.getExternalStoragePublicDirectory(
		Environment.DIRECTORY_DOWNLOADS),
		"OfflineMap/osmdroid"
		);
		
		if (!base.exists()) base.mkdirs();
		
		Configuration.getInstance().setOsmdroidBasePath(base);
		Configuration.getInstance().setOsmdroidTileCache(base);
	}
	
	// ================= RADIUS DOWNLOAD =================
	private void startDownload() {
		
		Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		
		if (loc == null) {
			status.setText("Location not found");
			return;
		}
		
		GeoPoint center = new GeoPoint(loc.getLatitude(), loc.getLongitude());
		
		map.getController().setCenter(center);
		map.getController().setZoom(14);
		
		status.setText("Downloading to External Storage...");
		
		new Thread(() -> {
			
			try {
				
				
				
				// force tile loading in radius
				for (int i = 0; i < 25; i++) {
					
					runOnUiThread(() -> {
						map.getController().zoomIn();
						map.getController().zoomOut();
						map.invalidate();
					});
					
					Thread.sleep(300);
				}
				
				runOnUiThread(() ->
				status.setText("Saved in /Download/OfflineMap ✔")
				);
				
				} catch (Exception e) {
				e.printStackTrace();
			}
			
		}).start();
	}
}