package za.org.samac.harvest;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import za.org.samac.harvest.adapter.MyData;
import za.org.samac.harvest.adapter.WorkerRecyclerViewAdapter;
import za.org.samac.harvest.adapter.collections;
import za.org.samac.harvest.domain.Worker;
import za.org.samac.harvest.util.WorkerComparator;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private static final String TAG = "Clicker";

    private ArrayList<Worker> workers;
    private ArrayList<Worker> workersSearch;
    private Map<Integer, Location> track;
    int trackCount = 0;
    boolean namesShowing = false;

    //used same names as IDs in xml
    private Button btnStart;
    private ProgressBar progressBar;
    private RelativeLayout relLayout;
    private RecyclerView recyclerView;//I used recycler view as the grid view duplicated and rearranged worker names
    private WorkerRecyclerViewAdapter adapter;
    private LocationManager locationManager;
    private Location location;
    //private FirebaseAuth mAuth;
    private boolean locationEnabled = false;
    private static final long LOCATION_REFRESH_TIME = 60000;
    private static final float LOCATION_REFRESH_DISTANCE = 3;
    private Date startSessionTime;
    private Date endSessionTime;
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private String currentUserEmail;
    private String userUid;
    public static String childKey;

    FirebaseDatabase database;
    DatabaseReference ref;//Firebase reference to workers collection
    DatabaseReference myRef;//Firebase reference to yields collection
    Query q;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        workers = new ArrayList<>();//stores worker names
        workersSearch = new ArrayList<>();//stores worker names
        adapter = new WorkerRecyclerViewAdapter(getApplicationContext(), workersSearch);

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        } else {
            locationEnabled = true;
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE, mLocationListener);
            location = locationManager
                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
            adapter.setLocation(location);
        }

        locationPermissions();
        new LocationHelper().getLocation(this);

        database = FirebaseDatabase.getInstance();
        ref = database.getReference("workers");//Firebase reference
        q = ref.orderByChild("name");
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null && dataSnapshot.getKey() != null) {
                    collectWorkers((Map<String, Object>) dataSnapshot.getValue(), dataSnapshot.getKey());
                }
                progressBar.setVisibility(View.GONE);//remove progress bar
                relLayout.setVisibility(View.VISIBLE);
                //user pressed start and all went well with retrieving data
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Error", databaseError.toString());
                progressBar.setVisibility(View.GONE);
                relLayout.setVisibility(View.VISIBLE);
            }
        });

        setContentView(R.layout.activity_main);//stet R to be referenced from specified xml file


        relLayout = findViewById(R.id.relLayout);
        progressBar = findViewById(R.id.progressBar);
        btnStart = findViewById(R.id.button_start);
        btnStart.setTag("green");//it is best not to use the tag to identify button status

        progressBar.setVisibility(View.VISIBLE);//put progress bar until data is retrieved from firebase
        relLayout.setVisibility(View.GONE);

        recyclerView = findViewById(R.id.recyclerView);//this encapsulates the worker buttons, it is better than gridview
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getApplicationContext(), 2);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, GridLayoutManager.VERTICAL));

        recyclerView.setAdapter(adapter);
        recyclerView.setVisibility(View.GONE);

        //bottom navigation bar
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.actionYieldTracker:

                            case R.id.actionSettings:

                            case R.id.actionLastSession:

                        }
                        return true;
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);
        MenuItem searchMenu = menu.findItem(R.id.search);
        final SearchView searchView = (SearchView) searchMenu.getActionView();
        searchView.setIconified(false);
        searchView.requestFocusFromTouch();
        searchView.setOnQueryTextListener(this);
        searchMenu.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                return true;
            }
        });
        return true;
    }

    /***********************
     ** Function below creates arrays of the workers, how many bags they collect
     * and an array of buttons to be added to the view
     */


    protected void collectWorkers(Map<String, Object> users, Object key) {
        for (Map.Entry<String, Object> entry : users.entrySet()) {
            Map singleUser = (Map) entry.getValue();
            String fullName = singleUser.get("name") + " " + singleUser.get("surname");
            Worker workerObj = new Worker();
            workerObj.setName(fullName);
            workerObj.setValue(0);
            workerObj.setID(entry.getKey());
            workers.add(workerObj);
        }

        Collections.sort(workers, new WorkerComparator());

        workersSearch.addAll(workers);
        //adapter.notifyDataSetChanged();
    }

    /*******************************
     Code below handles the stop/start button, runs a timer and displays how many
     bags were collected in the elapsed time. It then clears for another timer to start.
     Sessions for each worker still needs to be implemented *
     */
    long startTime = 0, stopTime = 0;

    @SuppressLint({"SetTextI18n", "MissingPermission"})
    public void onClickStart(View v) {
        userUid = user.getUid();
        myRef = database.getReference(userUid + "/sessions/");//path to sessions collection in Firebase
        childKey = myRef.push().getKey();//childByAutoKey


        currentUserEmail = user.getEmail();
        startSessionTime = Calendar.getInstance().getTime();//get time at the start of session

        recyclerView.setVisibility(View.VISIBLE);
        if (!namesShowing) {
            TextView textView = findViewById(R.id.startText);
            textView.setVisibility(View.GONE);
            namesShowing = true;
            adapter.notifyDataSetChanged();
        }
        if (btnStart.getTag() == "green") {
            adapter.setPlusEnabled(true);
            adapter.setMinusEnabled(true);
            track = new HashMap<Integer, Location>(); //used in firebase function
            track.put(trackCount, location);
            if (locationEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
            }
            startTime = System.currentTimeMillis();
            btnStart.setBackgroundColor(Color.parseColor("#FFFF8800"));
            btnStart.setText("Stop");
            btnStart.setTag("orange");
        } else {
            //TODO: check if app closes or crashes
            //TODO: save data to Firebase
            endSessionTime = Calendar.getInstance().getTime();//get time at the end of session

            stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;
            // do something with time
            int h = (int) ((elapsedTime / 1000) / 3600);
            int m = (int) (((elapsedTime / 1000) / 60) % 60);
            int s = (int) ((elapsedTime / 1000) % 60);
            //this is the output of the pop up when the user clicks stop (the session)
            String timeTaken = h + " hour(s), " + m + " minute(s) and " + s + " second(s)";
            String msg = "A total of " + adapter.totalBagsCollected + " bags have been collected in " + timeTaken + ".";
            if (locationEnabled) {
                locationManager.removeUpdates(mLocationListener);
            }

            adapter.totalBagsCollected = 0;//reset total number of bags collected for all workers
            for(int i = 0 ; i < workers.size() ; i++) {
                workers.get(i).setValue(0);
            }
            adapter.setPlusEnabled(false);
            adapter.setMinusEnabled(false);
            collections collectionObj = adapter.getCollectionObj();
            collectionObj.sessionEnd();
            //****writeToFirebase(collectionObj);
            //pop up is used to show how many bags were collected in the elapsed time
            AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
            dlgAlert.setMessage(msg);
            dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    adapter.setIncrement();
                    dialog.dismiss();
                }
            });
            dlgAlert.setCancelable(false);
            dlgAlert.create().show();

            btnStart.setBackgroundColor(Color.parseColor("#FF0CCB29"));

            btnStart.setText("Start");
            btnStart.setTag("green");
        }
    }

    /*private void addYield(String userId, String username, String title, String body) {
        // Create new post at /user-posts/$userid/$postid and at
        // /posts/$postid simultaneously
        database = FirebaseDatabase.getInstance();
        userUid = user.getUid();
        myRef = database.getReference(userUid + "/sessions/");

        childKey = myRef.push().getKey();//childByAutoKey

        //String key = myRef.push().getKey();

        Map<String, Object> parentHashcollections = new HashMap<>();
        parentHashcollections.put(childKey + "/collections/", parentHashcollections);
        //childUpdates.put(childKey + "/email", currentUserEmail);
        childUpdates.put(childKey + "/end_date", startSessionTime);
        childUpdates.put(childKey + "/start_date", endSessionTime);

        Map<String, Object> parentHashtrack = new HashMap<>();

        Map<String, Object> hashcoordinates = new HashMap<>();

        parentHashcollections.put("getWorkerID/incrementID or index/", hashcoordinates);
        parentHashcollections.put("getWorkerID/incrementID or index/timeWhenPlusWasClicked", time);



        parentHashcollections.put("childByAutoKey/email", currentUserEmail);

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(childKey + "/collections/", parentHashcollections);
        childUpdates.put(childKey + "/email", currentUserEmail);
        childUpdates.put(childKey + "/end_date", startSessionTime);
        childUpdates.put(childKey + "/start_date", endSessionTime);
        childUpdates.put(childKey + "/track/", parentHashtrack);

        myRef.updateChildren(childUpdates);


        //database.collection("cities").document(key).set(childUpdates, SetOptions.merge());

        myRef.updateChildren(childUpdates);
    }*/

    private void writeToFirebase(collections collectionObj) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        String key = database.getReference("yields").push().getKey();
        DatabaseReference mRef = database.getReference().child("yields").child(key);
        mRef.setValue("collections");
        Map<String, MyData> map = collectionObj.getIndividualCollections();
        Map<String, String> collectionData = new HashMap<String, String>();
        collectionData.put("email", collectionObj.getForemanEmail());
        collectionData.put("end_date", Double.toString(collectionObj.getEnd_date()));
        collectionData.put("start_date", Double.toString(collectionObj.getStart_date()));
        mRef.child("collections").setValue(collectionData);
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            mRef.child("collections").setValue(pair.getKey());
            MyData data = map.get(pair.getKey());
            DatabaseReference dRef = mRef.child("collections").child((String) pair.getKey());
            for (int i = 0; i < data.size; i++) {
                dRef.setValue(Integer.toString(i));
                dRef.child(Integer.toString(i)).setValue("coord");
                dRef.child(Integer.toString(i)).setValue("date");
                dRef.child("date").setValue(data.date.get(i));
                dRef.child("coord").setValue("lat");
                dRef.child("coord").setValue("lng");
                dRef.child("coord").child("lat").setValue(data.latitude.get(i));
                dRef.child("coord").child("lng").setValue(data.longitude.get(i));
            }
        }
        /*mRef.child("collections").setValue("email");
        mRef.child("collections").setValue("start_date");
        mRef.child("collections").setValue("end_date");
        mRef.child("collections").child("email").setValue(collectionObj.getForemanEmail());
        mRef.child("collections").child("start_date").setValue(collectionObj.getStart_date());
        mRef.child("collections").child("end_date").setValue(collectionObj.getEnd_date());*/
        mRef.setValue("track");
        Iterator it1 = track.entrySet().iterator();
        int count = 0;
        while (it1.hasNext()) {
            Map.Entry pair = (Map.Entry) it1.next();
            Location loc = (Location) pair.getValue();
            if (loc != null) {
                double lat = loc.getLatitude();
                double lng = loc.getLongitude();
                mRef.child("track").setValue(Integer.toString(count));
                mRef.child("track").child(Integer.toString(count)).setValue("lat");
                mRef.child("track").child(Integer.toString(count)).setValue("lng");
                mRef.child("track").child(Integer.toString(count)).child("lat").setValue(lat);
                mRef.child("track").child(Integer.toString(count)).child("lng").setValue(lng);
            }
            ++count;
        }
    }

    public Location getLocation() {
        return location;
    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location locationChange) {
            location = locationChange;
            trackCount++;
            track.put(trackCount, location);
            adapter.setLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };


    @Override
    public boolean onQueryTextSubmit(String s) {
        doWorkersClientSideSearch(s);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        doWorkersClientSideSearch(s);
        return false;
    }

    public void locationPermissions() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 101);

        }
    }

    private void doWorkersClientSideSearch(String searchText) {
        workersSearch.clear();
        if(searchText != null && !searchText.equals("")) {
            List<Worker> results = new ArrayList<>();
            for (Worker worker : workers) {
                if(worker.getName().toLowerCase().contains(searchText.toLowerCase())){
                    results.add(worker);
                }
            }
            workersSearch.addAll(results);
        } else {
            workersSearch.addAll(workers);
        }
        adapter.notifyDataSetChanged();
    }
}