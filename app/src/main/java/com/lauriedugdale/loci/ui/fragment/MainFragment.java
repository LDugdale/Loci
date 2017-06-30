package com.lauriedugdale.loci.ui.fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.text.Text;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.GridBasedAlgorithm;
import com.lauriedugdale.loci.EntryItem;
import com.lauriedugdale.loci.EventIconRendered;
import com.lauriedugdale.loci.data.DataUtils;
import com.lauriedugdale.loci.data.dataobjects.FilterOptions;
import com.lauriedugdale.loci.data.dataobjects.GeoEntry;
import com.lauriedugdale.loci.R;
import com.lauriedugdale.loci.ui.activity.AugmentedActivity;
import com.lauriedugdale.loci.ui.activity.NotificationActivity;
import com.lauriedugdale.loci.ui.activity.entry.AudioEntryActivity;
import com.lauriedugdale.loci.ui.activity.FullScreenActivity;
import com.lauriedugdale.loci.ui.activity.entry.ImageEntryActivity;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Created by mnt_x on 28/05/2017.
 */

public class MainFragment extends BaseFragment implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener {

    private static float MAXIMUM_DISTANCE = 50.0f; //maximum distance
    private boolean mIsWithinBounds; // check if markers is within MAXIMUM_DISTANCE

    // location variables
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mCurrentLocation;

    // potential map types its possible to display
    private final int[] MAP_TYPES = { GoogleMap.MAP_TYPE_SATELLITE,
            GoogleMap.MAP_TYPE_NORMAL,
            GoogleMap.MAP_TYPE_HYBRID,
            GoogleMap.MAP_TYPE_TERRAIN,
            GoogleMap.MAP_TYPE_NONE };

    private int curMapTypeIndex = 1; // chosen map type from MAP_TYPES

    private DataUtils mDataUtils; // handles data transactions with firebase

    private HashMap<String, EntryItem> visibleMarkers; // keeps track of visible markers
    private HashMap<String, GeoEntry> mEntryMap; // keeps track of the entries downloaded from the server
    private ClusterManager<EntryItem> mClusterManager;

    private FusedLocationProviderClient mFusedLocationClient; // used for getting the current lcoation

    private GeoEntry mCurrentEntry; // currently selected marker

    // time controls
    private final static String DATE_TIME = "yyyy-MM-dd HH:mm:ss";
    private final static String DATE = "dd-MM-yyyy";
    private SimpleDateFormat mDateTime;
    private Calendar mCalendar;
    private TextView mDisplayFromDate;
    private TextView mDisplayToDate;
    private DatePickerDialog mFromTimePicker;
    private DatePickerDialog mToTimePicker;
    private Button mFilterButton;

    // filter variables
    private FilterOptions mFilterOptions;
    private FilterOptions mTempFilterOptions;

    private FrameLayout mMainLayout;

    /**
     * Used to return fragment for viewpager quickly
     * @return
     */
    public static MainFragment create(){
        return new MainFragment();
    }


    public GoogleMap getMap() {
        return mMap;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // instantiate inital variables
        mDataUtils = new DataUtils(getActivity());
        visibleMarkers = new HashMap<String, EntryItem>();
        mEntryMap = new HashMap<String, GeoEntry>();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
        mFilterOptions = new FilterOptions();


        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        // set up google maps API
        mGoogleApiClient = new GoogleApiClient.Builder( getActivity() )
                .addConnectionCallbacks( this )
                .addOnConnectionFailedListener( this )
                .addApi( LocationServices.API )
                .build();

        mMainLayout = (FrameLayout) getActivity().findViewById(R.id.main_layout);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem addFriendItem = menu.findItem(R.id.action_add_friend);
        MenuItem notificationItem = menu.findItem(R.id.action_notification);
        MenuItem addGroupItem = menu.findItem(R.id.action_add_group);

        addGroupItem.setVisible(false);
        addFriendItem.setVisible(false);
        notificationItem.setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_filter) {
            showSelectFriendsPopup(mMainLayout);
        }


        if (id == R.id.action_ar) {
            Intent intent = new Intent(getActivity(), AugmentedActivity.class);
            startActivity(intent);

            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        if (mMap != null) {
            return;
        }
        // get map fragment
        SupportMapFragment smf = ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map));
        smf.getMapAsync(this);
    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_main;
    }

    @Override
    public void inOnCreateView(View root, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // instantiate the google map field variable
        mMap = googleMap;

        // configure cluster manager
        mClusterManager = new ClusterManager<EntryItem>(getActivity(), mMap);
        final EventIconRendered rendered = new EventIconRendered(getActivity().getApplicationContext(), googleMap, mClusterManager);
        mClusterManager.setRenderer(rendered);

        mClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<EntryItem>() {
            @Override
            public boolean onClusterItemClick(EntryItem entryItem) {
                System.out.println("onClusterItemClick");
                checkDistance(entryItem.getPosition().latitude, entryItem.getPosition().longitude);
                mCurrentEntry = entryItem.getGeoEntry();
                showMarkerInfoPopup(mMainLayout);
                return true;
            }
        });

        // setup listeners
        getMap().setOnCameraIdleListener(mClusterManager);
        getMap().setOnMarkerClickListener(mClusterManager);
        getMap().setOnCameraIdleListener(getCameraIdleListener());
        getMap().setOnMapLongClickListener(this);
        getMap().setOnInfoWindowClickListener(this);
        getMap().setOnMapClickListener(this);
    }

    private GoogleMap.OnCameraIdleListener getCameraIdleListener() {
        return new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                mDataUtils.readAllEntries(bounds.southwest.latitude,
                        bounds.northeast.latitude,
                        mFilterOptions.getNumericalFromDate(),
                        mFilterOptions.getNumericalToDate(),
                        mFilterOptions.getCheckedTypes(),
                        mEntryMap);
                addEntryToMap();
            }
        };
    }

    //Note that the type "Items" will be whatever type of object you're adding markers for so you'll
    //likely want to create a List of whatever type of items you're trying to add to the map and edit this appropriately
    //Your "Item" class will need at least a unique id, latitude and longitude.
    private void addEntryToMap() {

        if(this.mMap != null) {
            //This is the current user-viewable region of the map
            LatLngBounds bounds = this.mMap.getProjection().getVisibleRegion().latLngBounds;

            //Loop through all the items that are available to be placed on the map
            Iterator it = mEntryMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                GeoEntry entry = (GeoEntry) pair.getValue();


                if(bounds.contains(new LatLng(entry.getLatitude(), entry.getLongitude()))) {
                    //If the item isn't already being displayed
                    if(!visibleMarkers.containsKey(entry.getEntryID())) {
                        //Add the Marker to the Map and keep track of it with the HashMap
                        //getMarkerForItem just returns a MarkerOptions object

                        EntryItem marker = new EntryItem(entry.getLatitude(), entry.getLongitude(), entry.getTitle(), entry.getFileType(), entry);
                        visibleMarkers.put(entry.getEntryID(), marker);
                        mClusterManager.addItem(marker);
                        mClusterManager.cluster();



                    }
                } else {
                    //If the course was previously on screen
                    if(visibleMarkers.containsKey(entry.getEntryID())) {
                        //1. Remove the Marker from the GoogleMap
//                        visibleMarkers.get(entry.getEntryID()).remove();
                        mClusterManager.removeItem(visibleMarkers.get(entry.getEntryID()));
                        mClusterManager.cluster();

                        visibleMarkers.remove(entry.getEntryID());

                        //2. Remove the reference to the Marker from the HashMap
                        visibleMarkers.remove(entry.getEntryID());
                        // remove from iterator
                        it.remove();
                    }
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if( mGoogleApiClient != null && mGoogleApiClient.isConnected() ) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mCurrentLocation = LocationServices
                .FusedLocationApi
                .getLastLocation( mGoogleApiClient );

        initCamera( mCurrentLocation );
    }

    private void initCamera( Location location ) {
        CameraPosition position = CameraPosition.builder()
                .target( new LatLng( location.getLatitude(),
                        location.getLongitude() ) )
                .zoom( 16f )
                .bearing( 0.0f )
                .tilt( 0.3f )
                .build();

        getMap().animateCamera( CameraUpdateFactory.newCameraPosition( position ), null );

        getMap().setMapType( MAP_TYPES[curMapTypeIndex] );
        getMap().getUiSettings().setCompassEnabled(false);
        getMap().getUiSettings().setMapToolbarEnabled(false);
        getMap().setBuildingsEnabled(false);
        getMap().setMyLocationEnabled(true);
        getMap().getUiSettings().setMyLocationButtonEnabled(false);

//        getMap().getUiSettings().setZoomControlsEnabled( true );
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    @Override
    public void onMapClick(LatLng latLng) {
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        return false;
    }

    private float getDistanceInMeters(double lat1, double lng1, double lat2, double lng2) {
        float [] dist = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, dist);
        return dist[0];
    }

    private Class getDestination(){
        Class destination = null;
        switch(mCurrentEntry.getFileType()){
            case DataUtils.IMAGE:
                destination = ImageEntryActivity.class;
                break;
            case DataUtils.AUDIO:
                destination = AudioEntryActivity.class;
                break;
            default:
                break;
        }

        return destination;
    }

    private void checkDistance(final double markerLat, final double markerLng){

        mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    Float distance = getDistanceInMeters(location.getLatitude(), location.getLongitude(), markerLat, markerLng);
                    mIsWithinBounds = (distance <= MAXIMUM_DISTANCE);
                }
            }
        });
    }

    public void showMarkerInfoPopup(View anchorView) {
        View popupView = getActivity().getLayoutInflater().inflate(R.layout.popup_map_entry_info, null);

        // PopupWindow popupWindow = new PopupWindow(popupView, RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT);
        final PopupWindow popupWindow = new PopupWindow(popupView, RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT , true);

        // If the PopupWindow should be focusable
        popupWindow.setFocusable(true);
        // If you need the PopupWindow to dismiss when when touched outside
        popupWindow.setBackgroundDrawable(new ColorDrawable());

        int location[] = new int[2];

        // Get the View's(the one that was clicked in the Fragment) location
        anchorView.getLocationOnScreen(location);

        // Using location, the PopupWindow will be displayed right under anchorView
        popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0);

        // connect time UI elements
        TextView entryTitle = (TextView) popupView.findViewById(R.id.info_bar_title);
        ImageView entryImage = (ImageView) popupView.findViewById(R.id.info_bar_type);
        TextView showEntry = (TextView) popupView.findViewById(R.id.info_bar_show_entry);

        setInfoBarImage(entryImage, mCurrentEntry.getFileType());
        entryTitle.setText(mCurrentEntry.getTitle());

        showEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                if(!mIsWithinBounds){
//                    return;
//                }

                Intent startViewEntryIntent = new Intent(getActivity(), getDestination());
                startViewEntryIntent.putExtra(Intent.ACTION_OPEN_DOCUMENT, mCurrentEntry);
                getActivity().startActivity(startViewEntryIntent);
            }

        });

    }

    private void setInfoBarImage(ImageView imageType, int type){
        // remove previous listener
        imageType.setOnClickListener(null);
        switch(type){
            case DataUtils.NO_MEDIA:
                imageType.setImageDrawable(getResources().getDrawable(R.drawable.ic_text));
                break;
            case DataUtils.IMAGE:
                imageType.setImageDrawable(getResources().getDrawable(R.drawable.ic_image));
                break;
            case DataUtils.AUDIO:
                imageType.setImageDrawable(getResources().getDrawable(R.drawable.ic_audiotrack));
                break;
            default:
                break;
        }
    }

    public void showSelectFriendsPopup(View anchorView) {

        // TODO check boxes for media types and date pickers to and from dates
        View popupView = getActivity().getLayoutInflater().inflate(R.layout.popup_filter, null);

        // PopupWindow popupWindow = new PopupWindow(popupView, RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT);
        final PopupWindow popupWindow = new PopupWindow(popupView, RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT , true);

        // If the PopupWindow should be focusable
        popupWindow.setFocusable(true);
        // If you need the PopupWindow to dismiss when when touched outside
        popupWindow.setBackgroundDrawable(new ColorDrawable());

        int location[] = new int[2];

        // Get the View's(the one that was clicked in the Fragment) location
        anchorView.getLocationOnScreen(location);

        // Using location, the PopupWindow will be displayed right under anchorView
        popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0);

        // connect time UI elements
        mDisplayFromDate = (TextView) popupView.findViewById(R.id.display_from_date);
        mDisplayToDate = (TextView) popupView.findViewById(R.id.display_to_date);

        final CheckBox checkBoxImage = (CheckBox) popupView.findViewById(R.id.checkbox_image);
        final CheckBox checkBoxAudio = (CheckBox) popupView.findViewById(R.id.checkbox_audio);
        Button button = (Button) popupView.findViewById(R.id.apply_filters);
        mTempFilterOptions = new FilterOptions();

        mDisplayToDate.setText(mFilterOptions.getToDate());
        mDisplayFromDate.setText(mFilterOptions.getFromDate());

        checkBoxImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkBoxImage.isChecked()){
                    mTempFilterOptions.getCheckedTypes().put(DataUtils.IMAGE, true);
                } else {
                    mTempFilterOptions.getCheckedTypes().put(DataUtils.IMAGE, false);
                }
            }
        });

        checkBoxAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkBoxAudio.isChecked()){
                    mTempFilterOptions.getCheckedTypes().put(DataUtils.AUDIO, true);
                } else {
                    mTempFilterOptions.getCheckedTypes().put(DataUtils.AUDIO, false);
                }
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mFilterOptions.setFromDate(mTempFilterOptions.getFromDate());
                mFilterOptions.setToDate(mTempFilterOptions.getToDate());
                mFilterOptions.setCheckedTypes(mTempFilterOptions.getCheckedTypes());
                popupWindow.dismiss();
            }
        });

        dateTimeListeners();
    }

    public void dateTimeListeners(){

        mDateTime = new SimpleDateFormat(DATE_TIME, Locale.UK);

        mCalendar = Calendar.getInstance();


        // listner for text field, launches android calendar
        mDisplayFromDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFromTimePicker.show();
            }
        });

        // listner for text field, launches android calendar
        mDisplayToDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mToTimePicker.show();
            }
        });


        // date listner update when changed
        mFromTimePicker = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mCalendar.set(year, monthOfYear, dayOfMonth);
                mDateTime.applyPattern(DATE);
                String fromDate = mDateTime.format(mCalendar.getTime());
                mDisplayFromDate.setText(fromDate);
                mTempFilterOptions.setFromDate(fromDate);
            }

        },mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));

        // date listner update when changed
        mToTimePicker = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mCalendar.set(year, monthOfYear, dayOfMonth);
                mDateTime.applyPattern(DATE);
                String toDate = mDateTime.format(mCalendar.getTime());
                mDisplayToDate.setText(toDate);
                mTempFilterOptions.setToDate(toDate);
            }

        },mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));
    }
}
