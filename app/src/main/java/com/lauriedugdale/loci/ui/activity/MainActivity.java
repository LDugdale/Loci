package com.lauriedugdale.loci.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.icu.util.Calendar;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lauriedugdale.loci.R;
import com.lauriedugdale.loci.data.DataUtils;
import com.lauriedugdale.loci.data.dataobjects.User;
import com.lauriedugdale.loci.services.LociLocationService;
import com.lauriedugdale.loci.ui.activity.auth.LoginActivity;
import com.lauriedugdale.loci.ui.adapter.MainActivityAdapter;
import com.lauriedugdale.loci.ui.nav.LociNavView;

import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;

    private TextView mUsername;
    private TextView mEmail;
    private ImageView mMenuProfileImage;

    private Context mContext = this;

    private FirebaseAuth mAuth;

    private DataUtils mDataUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDataUtils = new DataUtils(this);

        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HHmmss", Locale.UK);
        String currentDateandTime = sdf.format(c.getTime());
        Long dateInLong = 0L;
        try {
            Date date = sdf.parse(currentDateandTime);
            dateInLong = date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Date date = new Date(dateInLong);
        Format format = new SimpleDateFormat("yyyyMMdd HHmmss", Locale.UK);

        format.format(date);
        mAuth = FirebaseAuth.getInstance();

        //find view pager and Adapter for managing fragments
        ViewPager viewPager = (ViewPager) findViewById(R.id.am_view_pager);
        MainActivityAdapter adapter = new MainActivityAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);

        // find navigation tabs and start listeners
        LociNavView tabsView = (LociNavView) findViewById(R.id.am_loci_tabs);
        tabsView.setUpWithViewPager(viewPager, this);

        // set initial fragment
        viewPager.setCurrentItem(1);

//        // start location service
//        Intent intent = new Intent(this, LociLocationService.class);
//        startService(intent);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        initNavigationDrawer();

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            public void setUpWithViewPager(final ViewPager viewPager, final Context context) {
                viewPager.addOnPageChangeListener(this);
            }
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == 0) {
                    mToolbar.setBackgroundColor(ContextCompat.getColor(mContext, R.color.white));
                    mToolbar.getBackground().setAlpha((int) (positionOffset * 100));
                } else if (position == 1){
                     mToolbar.getBackground().setAlpha(0);
                } else if (position == 2){
                    mToolbar.setBackgroundColor(ContextCompat.getColor(mContext, R.color.white));

                }
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    public void initNavigationDrawer() {

        NavigationView navigationView = (NavigationView)findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                int id = menuItem.getItemId();

                switch (id){
                    case R.id.logout:
                        mAuth.signOut();

                        // this listener will be called when there is change in firebase user session
                        FirebaseAuth.AuthStateListener authListener = new FirebaseAuth.AuthStateListener() {
                            @Override
                            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                                FirebaseUser user = firebaseAuth.getCurrentUser();
                                if (user == null) {
                                    // user auth state is changed - user is null
                                    // launch login activity
                                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                }
                            }
                        };
//                        finish();
                        break;

                }
                return true;
            }
        });


        View header = navigationView.getHeaderView(0);
        mUsername = (TextView)header.findViewById(R.id.tv_username);
        mEmail = (TextView)header.findViewById(R.id.tv_email);
        mMenuProfileImage = (ImageView) header.findViewById(R.id.menu_profile_image);

        mDataUtils.getProfilePic(mMenuProfileImage, R.drawable.default_profile);

        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("users");
        ref.child(mDataUtils.getCurrentUID()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    mUsername.setText(user.getUsername());
                    mEmail.setText(user.getEmail());

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });


        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer);

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this,mDrawerLayout,mToolbar,R.string.openDrawer,R.string.closeDrawer){

            @Override
            public void onDrawerClosed(View v){
                super.onDrawerClosed(v);
            }

            @Override
            public void onDrawerOpened(View v) {
                super.onDrawerOpened(v);
            }
        };
        mDrawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add_friend) {
            Intent intent = new Intent(this, SelectFriend.class);
            startActivity(intent);

            return true;
        }


        return super.onOptionsItemSelected(item);
    }
}
