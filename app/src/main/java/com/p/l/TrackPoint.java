/*


package com.p.l;

public class TrackPoint {
	public double lat;
	public double lng;
	public float speed;
	public long time;
	public double segmentDistance;
	public double totalDistance;
}

  */
  
  
package com.p.l;

public class TrackPoint {
	
	public double lat;
	public double lng;
	public float speed;
	public long time;
	public double segmentDistance;
	public double totalDistance;
	
	// ✅ Empty constructor (important for Gson)
	public TrackPoint() {}
	
	// ✅ Full constructor (jo tum use kar rahe ho)
	public TrackPoint(double lat, double lng, float speed,
	long time, double segmentDistance, double totalDistance) {
		this.lat = lat;
		this.lng = lng;
		this.speed = speed;
		this.time = time;
		this.segmentDistance = segmentDistance;
		this.totalDistance = totalDistance;
	}
}