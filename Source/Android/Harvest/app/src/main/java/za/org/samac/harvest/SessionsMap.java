package za.org.samac.harvest;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import za.org.samac.harvest.adapter.MyData;
import za.org.samac.harvest.adapter.SessionDetails;
import za.org.samac.harvest.domain.Worker;
import za.org.samac.harvest.util.AppUtil;
import za.org.samac.harvest.util.Data;
import za.org.samac.harvest.util.Orchard;

import static za.org.samac.harvest.MainActivity.getWorkers;

public class SessionsMap extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    //private ArrayList<Marker> markersArray = new ArrayList<Marker>();
    private String uid;
    private FirebaseDatabase database;
    private DatabaseReference sessionsRef;
    private DatabaseReference collectionsRef;
    private DatabaseReference workerRef;
    private Query q;
    private Map<String, String> workerID;
    private ArrayList<Worker> workers;
    private boolean isFirstCollection;
    private boolean isHere = false;
    private boolean isThere = false;
    private LatLng moveMapHere ; // just used to find where to move map to
    private PolylineOptions polyline;
    private ArrayList<MarkerOptions> pickups;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sessions_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //bottom nav bar
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.actionSession);
            BottomNavigationViewHelper.removeShiftMode(bottomNavigationView);

            bottomNavigationView.setOnNavigationItemSelectedListener(
                    new BottomNavigationView.OnNavigationItemSelectedListener() {
                        @Override
                        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.actionYieldTracker:
                                    Intent openMainActivity = new Intent(SessionsMap.this, MainActivity.class);
                                    openMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                    startActivityIfNeeded(openMainActivity, 0);
                                    return true;
                                case R.id.actionInformation:
                                    startActivity(new Intent(SessionsMap.this, InformationActivity.class));
                                    return true;
                                case R.id.actionSession:
                                    return true;
                                case R.id.actionStats:
                                    startActivity(new Intent(SessionsMap.this, Stats.class));
                                    return true;
                            }
                            return true;
                        }
                    });
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.actionSession);//set correct item to pop out on the nav bar
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        isFirstCollection = true; // just used to find where to move map to
        uid = user.getUid();

        mMap.clear();

        PolylineOptions polyline = new PolylineOptions();
        polyline.color(Color.BLUE);
        boolean first = true;
        for (Location loc : Sessions.selectedItem.track) {
            LatLng ll = new LatLng(loc.getLatitude(), loc.getLongitude());
            if (first) {
                moveMapHere = ll;
                first = false;
            }
            polyline.add(ll);
        }
        mMap.addPolyline(polyline);

        HashMap<String, ArrayList<Pickup>> cols = (HashMap<String, ArrayList<Pickup>>) Sessions.selectedItem.collectionPoints;
        for (String key : cols.keySet()) {
            ArrayList<Pickup> data = cols.get(key);

            for (int i = 0; i < data.size(); i++) {
                LatLng ll = new LatLng(data.get(i).lat, data.get(i).lng);
                if (first) {
                    moveMapHere = ll;
                    first = false;
                }
                mMap.addMarker(new MarkerOptions().position(ll).title(data.get(i).workerName));
            }
        }

        Data data = new Data();
        for (Orchard orchard : data.getOrchards()) {
            if (!orchard.getCoordinates().isEmpty()) {
                PolygonOptions polygon = new PolygonOptions();
                polygon.fillColor(0x110000FF);
                polygon.strokeColor(0x550000FF);
                polygon.strokeWidth(3);

                for (LatLng coord : orchard.getCoordinates()) {
                    polygon.add(coord);
                }

                mMap.addPolygon(polygon);
            }
        }

        callCameraMove();
    }

    private void callCameraMove() {
        if (moveMapHere!=null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(moveMapHere, 15));
        }
    }

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
            return false;
        }
        return true;
    }
}
