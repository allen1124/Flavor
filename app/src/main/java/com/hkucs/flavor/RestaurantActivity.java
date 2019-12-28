package com.hkucs.flavor;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RestaurantActivity extends AppCompatActivity {

    private static final String TAG = "RestaurantActivity";
    private static final String BOOKMARK_PREF = "BOOKMARK_PREF";
    private static final int GOOGLE_SIGN_IN = 1111;
    private static final String LIKE = "1";
    private static final String DISLIKE = "-1";
    private static final String UNDETERMINED = "0";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
    private String userLike = UNDETERMINED;
    private Place restaurant;
    private ArrayList<Comment> comments = new ArrayList<>();
    private Toolbar toolbar;
    private FloatingActionButton fabDirection;
    private AppBarLayout appbar;
    private ImageView userAvatar;
    private TextView username;
    private RatingBar overallRating;
    private TextView vinicity;
    private TextView openNow;
    private ImageView likeIcon;
    private TextView likeCount;
    private ImageView dislikeIcon;
    private TextView dislikeCount;
    private RatingBar commentRating;
    private EditText commentText;
    private SignInButton signInButton;
    private Button commentPost;
    private RecyclerView commentView;
    private LinearLayoutManager layoutManager;
    private CommentsAdapter commentsAdapter;
    private CircleImageView bookmarkIcon;
    private boolean bookmarked = false;

    SharedPreferences bookmarkPref;
    GoogleSignInClient mGoogleSignInClient;
    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference dbRef = database.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        Intent intent = getIntent();
        restaurant = (Place) intent.getSerializableExtra("place");
        setContentView(R.layout.activity_restaurant);
        appbar = (AppBarLayout) findViewById(R.id.app_bar);
        if(intent.getStringExtra("photo") != null){
            Bitmap bmp = null;
            try {
                FileInputStream is = openFileInput(intent.getStringExtra("photo")+".png");
                bmp = BitmapFactory.decodeStream(is);
                appbar.setBackground(new BitmapDrawable(this.getResources(), addGradient(bmp)));
                is.close();
            } catch (Exception e) {
                Log.d(TAG, "No cached photo");
            }
        }
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(restaurant.getName());
        userAvatar = (ImageView) findViewById(R.id.c_avatar_imageView);
        username = (TextView) findViewById(R.id.c_username_textView);
        commentRating = (RatingBar) findViewById(R.id.c_user_comment_ratingBar);
        commentText = (EditText) findViewById(R.id.user_comment_editText);
        overallRating = (RatingBar) findViewById(R.id.overall_rating);
        vinicity = (TextView) findViewById(R.id.vinicity_textView);
        openNow = (TextView) findViewById(R.id.open_now_textView);
        likeIcon = (ImageView)findViewById(R.id.like_imageView);
        likeCount = (TextView) findViewById(R.id.like_count_textView);
        dislikeIcon = (ImageView) findViewById(R.id.dislike_imageView);
        dislikeCount = (TextView) findViewById(R.id.dislike_count_textView);
        bookmarkIcon = (CircleImageView) findViewById(R.id.bookmark_imageView);
        fabDirection = (FloatingActionButton) findViewById(R.id.fab);
        signInButton = (SignInButton) findViewById(R.id.user_sign_in_button);
        commentPost = (Button) findViewById(R.id.user_comment_post_button);
        commentView = (RecyclerView) findViewById(R.id.comment_recycler_view);
        commentView.setNestedScrollingEnabled(false);
        layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, true);
        commentView.setLayoutManager(layoutManager);
        setGooglePlusButtonText(signInButton, getResources().getString(R.string.comment_sign_in));
        vinicity.setText(restaurant.getVicinity());
        if(restaurant.getOpenNow() == null){
            openNow.setVisibility(View.INVISIBLE);
        }else{
            openNow.setVisibility(View.VISIBLE);
            if(restaurant.getOpenNow().matches("true")) {
                openNow.setText(R.string.open_now);
                openNow.setTextColor(getResources().getColor(R.color.open));
            }else {
                openNow.setText(R.string.closed_now);
                openNow.setTextColor(getResources().getColor(R.color.closed));
            }
        }
        likeCount.setText(String.valueOf(restaurant.getLikeCount()));
        dislikeCount.setText(String.valueOf(restaurant.getDislikeCount()));
        overallRating.setRating(restaurant.getRating().floatValue());
        bookmarkPref = getSharedPreferences(BOOKMARK_PREF, MODE_PRIVATE);
        bookmarked = bookmarkPref.getBoolean(restaurant.getPlaceId(), false);
        if(bookmarked){
            bookmarkIcon.setImageResource(R.mipmap.ic_bookmark_on);
        }else{
            bookmarkIcon.setImageResource(R.mipmap.ic_bookmark_off);
        }
        bookmarkIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = bookmarkPref.edit();
                if(bookmarked){
                    bookmarkIcon.setImageResource(R.mipmap.ic_bookmark_off);
                    bookmarked = false;
                    editor.remove(restaurant.getPlaceId());
                }else{
                    bookmarkIcon.setImageResource(R.mipmap.ic_bookmark_on);
                    bookmarked = true;
                    editor.putBoolean(restaurant.getPlaceId(), true);
                }
                editor.commit();
            }
        });
        likeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(user == null)
                    return;
                if(userLike != null && userLike.matches("1")) {
                    postUserLikeData(UNDETERMINED, false);
                }else {
                    if(userLike != null && userLike.matches("-1")) {
                        postUserLikeData(LIKE, true);
                    }else {
                        postUserLikeData(LIKE, false);
                    }
                }
            }
        });
        dislikeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(user == null)
                    return;
                if(userLike != null && userLike.matches("-1")) {
                    postUserLikeData(UNDETERMINED, false);
                }else {
                    if(userLike != null && userLike.matches("1")) {
                        postUserLikeData(DISLIKE, true);
                    }else {
                        postUserLikeData(DISLIKE, false);
                    }
                }
            }
        });
        fabDirection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Direct to: "+restaurant.getName());
                Intent intent = null;
                try {
                    intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+ URLEncoder.encode(restaurant.getName(), "utf-8")+"&destination_place_id="+restaurant.getPlaceId()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                startActivity(intent);
            }
        });
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                likeIcon.setEnabled(false);
                dislikeIcon.setEnabled(false);
                signInGoogle();
            }
        });
        userAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(user == null)
                    return;
                logout();
            }
        });
        username.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(user == null)
                    return;
                logout();
            }
        });
        commentPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String commentString = commentText.getText().toString();
                Double commentRatingValue = (double) commentRating.getRating();
                if(commentString.matches("")){
                    Toast.makeText(RestaurantActivity.this, R.string.comment_alert, Toast.LENGTH_SHORT).show();
                    return;
                }else{
                    setCommentEditingEnabled(false);
                    postComment(commentString, commentRatingValue);
                    commentRating.setRating(0);
                    commentText.getText().clear();
                    setCommentEditingEnabled(true);
                }
            }
        });
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        updateUI((user != null));
        getRestaurantData();
        getCommentData(restaurant.getPlaceId());
    }

    private void getCommentData(String placeId){
        dbRef.child("comments").child(placeId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                comments.clear();
                for(DataSnapshot commentSnapshot : dataSnapshot.getChildren()){
                    Comment comment = commentSnapshot.getValue(Comment.class);
                    comments.add(comment);
                }
                commentsAdapter = new CommentsAdapter(comments, RestaurantActivity.this);
                commentView.setAdapter(commentsAdapter);
                commentsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void postComment(String commentString, Double ratingValue){
        String placeKey = restaurant.getPlaceId();
        String userKey = user.getUid();
        String key = dbRef.child("comments").child(placeKey).push().getKey();
        String timestamp = dateFormat.format(new Date());
        Comment comment = new Comment(key, userKey, placeKey, user.getDisplayName(), user.getPhotoUrl().toString(), ratingValue, commentString, timestamp);
        Map<String, Object> postValues = comment.toMap();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/comments/"+ placeKey+"/"+key, postValues);
        dbRef.updateChildren(childUpdates);
        dbRef.child("places").child(placeKey).child("ratingCount").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Integer ratingCount = mutableData.getValue(int.class);
                if (ratingCount == null) {
                    return Transaction.success(mutableData);
                }
                mutableData.setValue(ratingCount + 1);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {

            }
        });
        postRestaurantRating(placeKey, ratingValue);
    }

    private void postRestaurantRating(final String placeKey, final Double ratingValue){
        dbRef.child("places").child(placeKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                com.hkucs.flavor.Place dbRestaurant = dataSnapshot.getValue(com.hkucs.flavor.Place.class);
                if(dbRestaurant == null){
                    return;
                }
                int ratingCount = dbRestaurant.getRatingCount();
                Double overallRating = dbRestaurant.getRating();
                dbRef.child("places").child(placeKey).child("rating").setValue((overallRating*(ratingCount-1)+ratingValue)/ratingCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void updateUI(boolean logined){
        Log.d(TAG, "updateUI: logined: "+logined);
        if(logined == true){
            signInButton.setVisibility(View.GONE);
            username.setVisibility(View.VISIBLE);
            commentPost.setEnabled(true);
            if(user.getPhotoUrl() != null){
                Glide.with(this).load(user.getPhotoUrl()).apply(RequestOptions.circleCropTransform()).into(userAvatar);
            }
            username.setText(user.getDisplayName());
            getUserLikeData();
            likeIcon.setEnabled(true);
            dislikeIcon.setEnabled(true);
        }else{
            commentRating.setRating(0);
            commentText.getText().clear();
            userAvatar.setImageResource(R.drawable.ic_person);
            signInButton.setVisibility(View.VISIBLE);
            username.setVisibility(View.GONE);
            commentPost.setEnabled(false);
        }
        commentRating.setIsIndicator(!logined);
        commentText.setEnabled(logined);
    }

    private void postUserLikeData(String value, boolean switched){
        String key = restaurant.getPlaceId();
        if(userLike == null){
            userLike = UNDETERMINED;
        }
        if((value.matches(LIKE) && !userLike.matches(LIKE)) ||
                (value.matches(DISLIKE) && switched) ||
                (value.matches(UNDETERMINED) && userLike.matches(LIKE))){
            int delta = 1;
            if((value.matches(DISLIKE) && switched) || (value.matches(UNDETERMINED) && userLike.matches(LIKE))){
                delta = -1;
            }
            final int finalDelta = delta;
            dbRef.child("places").child(key).child("likeCount").runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                    Integer likeCount = mutableData.getValue(Integer.class);
                    if (likeCount == null) {
                        return Transaction.success(mutableData);
                    }
                    mutableData.setValue(likeCount + finalDelta);
                    return Transaction.success(mutableData);
                }
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                }
            });
        }
        if((value.matches(DISLIKE) && !userLike.matches(DISLIKE)) ||
                (value.matches(LIKE) && switched) ||
                (value.matches(UNDETERMINED) && userLike.matches(DISLIKE))){
            int delta = 1;
            if((value.matches(LIKE) && switched) || (value.matches(UNDETERMINED) && userLike.matches(DISLIKE))){
                delta = -1;
            }
            final int finalDelta = delta;
            dbRef.child("places").child(key).child("dislikeCount").runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                    Integer dislikeCount = mutableData.getValue(Integer.class);
                    if (dislikeCount == null) {
                        return Transaction.success(mutableData);
                    }
                    mutableData.setValue(dislikeCount + finalDelta);
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {

                }
            });
        }
        dbRef.child("likes").child(user.getUid()+"-"+key).setValue(value);
    }

    private void getUserLikeData(){
        dbRef.child("likes").child(user.getUid()+"-"+restaurant.getPlaceId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                userLike = value;
                updateLikeUI(value);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getRestaurantData(){
        dbRef.child("places").child(restaurant.getPlaceId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                com.hkucs.flavor.Place dbRestaurant = dataSnapshot.getValue(com.hkucs.flavor.Place.class);
                if(dbRestaurant == null){
                    Log.d(TAG, "get Restaurant DB: restaurant not found in db");
                    return;
                }
                dislikeCount.setText(String.valueOf(dbRestaurant.getDislikeCount()));
                likeCount.setText(String.valueOf(dbRestaurant.getLikeCount()));
                overallRating.setRating(dbRestaurant.getRating().floatValue());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void updateLikeUI(String value){
        if(user != null && (value == null || value.matches("0"))){
            likeIcon.setImageResource(R.drawable.ic_like_off);
            dislikeIcon.setImageResource(R.drawable.ic_dislike_off);
        }else if(value == null && user == null){
            likeIcon.setImageResource(R.drawable.ic_like);
            dislikeIcon.setImageResource(R.drawable.ic_dislike);
        }else if(value.matches("1")){
            likeIcon.setImageResource(R.drawable.ic_like);
            dislikeIcon.setImageResource(R.drawable.ic_dislike_off);
        }else if(value.matches("-1")){
            likeIcon.setImageResource(R.drawable.ic_like_off);
            dislikeIcon.setImageResource(R.drawable.ic_dislike);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    public void signInGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN);
    }

    private void logout(){
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Do you want to logout?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mAuth.signOut();
                        mGoogleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                updateUI(false);
                                updateLikeUI(null);
                            }
                        });
                        user = null;
                    }})
                .setNegativeButton(android.R.string.no, null).show();
    }

    protected void setGooglePlusButtonText(SignInButton signInButton, String buttonText) {
        for (int i = 0; i < signInButton.getChildCount(); i++) {
            View v = signInButton.getChildAt(i);
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                tv.setText(buttonText);
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if(account != null)
                    firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            user = mAuth.getCurrentUser();
                            updateUI(true);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Snackbar.make(findViewById(R.id.constraint_layout), "Authentication Failed, proceeds without login.", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void setCommentEditingEnabled(boolean enabled) {
        commentText.setEnabled(enabled);
        commentPost.setEnabled(enabled);
        commentRating.setEnabled(enabled);
    }

    public Bitmap addGradient(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap overlay = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);
        canvas.drawBitmap(src, 0, 0, null);
        Paint paint = new Paint();
        LinearGradient shader = new LinearGradient(0, 200, 0, h, 0xFFFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawRect(0, 200, w, h, paint);
        return overlay;
    }
}
