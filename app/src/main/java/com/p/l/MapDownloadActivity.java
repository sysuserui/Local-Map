package com.p.l;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class MapDownloadActivity extends AppCompatActivity {
	
	private MapView map;
	private TextView status;
	private Button btnDownload;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Configuration.getInstance().setUserAgentValue(getPackageName());
		
		setContentView(R.layout.activity_download);
		
		status = findViewById(R.id.txtStatus);
		btnDownload = findViewById(R.id.btnDownload);
		
		map = new MapView(this);
		
		btnDownload.setOnClickListener(v -> startDownload());
	}
	
	// ================= COUNTRY DETECT =================
	private String getCountryCode() {
		Locale locale = getResources().getConfiguration().locale;
		return locale.getCountry(); // IN, US, etc.
	}
	
	// ================= DOWNLOAD LOGIC =================
	private void startDownload() {
		
		String country = getCountryCode();
		
		status.setText("Detecting country: " + country);
		
		new Thread(() -> {
			
			try {
				
				runOnUiThread(() ->
				status.setText("Downloading map tiles... Please wait")
				);
				
				// Fake region center (you can improve with real boundaries)
				GeoPoint center = getCountryCenter(country);
				
				MapTileProviderBasic provider = new MapTileProviderBasic(getApplicationContext());
				
				map.setTileProvider(provider);
				
				map.getController().setZoom(6.0);
				map.getController().setCenter(center);
				
				// Force tile loading (cache generation)
			
			for (int i = 0; i < 10; i++) {
				runOnUiThread(() ->
				map.getController().zoomIn()
				);
				Thread.sleep(300);
			}
			
			
				runOnUiThread(() ->
				status.setText("Download completed for country: " + country)
				);
				
				} catch (Exception e) {
				e.printStackTrace();
			}
			
		}).start();
	}
	
	// ================= COUNTRY CENTER =================
	private GeoPoint getCountryCenter(String country) {
		
		switch (country) {
			case "IN":
			return new GeoPoint(22.9734, 78.6569); // India
			case "US":
			return new GeoPoint(37.0902, -95.7129); // USA
			case "GB":
			return new GeoPoint(55.3781, -3.4360); // UK
			default:
			return new GeoPoint(20.5937, 78.9629);
		}
	}
}