package com.hkucs.flavor;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import org.ocpsoft.prettytime.PrettyTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder>{

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
    private ArrayList<Comment> commentList;
    Context context;

    public CommentsAdapter(ArrayList<Comment> commentList, Context context) {
        this.context = context;
        this.commentList = commentList;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView avatar;
        public TextView username;
        public RatingBar rating;
        public TextView comment;
        public TextView pettyTime;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);
            avatar = (ImageView) itemView.findViewById(R.id.c_avatar_imageView);
            username = (TextView) itemView.findViewById(R.id.c_username_textView);
            rating = (RatingBar) itemView.findViewById(R.id.c_user_comment_ratingBar);
            rating.setIsIndicator(true);
            comment = (TextView) itemView.findViewById(R.id.c_user_comment_textView);
            pettyTime = (TextView) itemView.findViewById(R.id.c_petty_time_textView);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_comment, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Comment comment = commentList.get(position);
        if(comment.getUserPhoto() != null) {
            Glide.with(context).load(Uri.parse(comment.getUserPhoto())).apply(RequestOptions.circleCropTransform()).into(holder.avatar);
        }
        holder.username.setText(comment.getUsername());
        holder.rating.setRating(comment.getRating().floatValue());
        holder.comment.setText(comment.getComment());
        Date commentDate = null;
        try {
            commentDate = dateFormat.parse(comment.getTimestamp());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        holder.pettyTime.setText(new PrettyTime().format(commentDate));
    }

    @Override
    public int getItemCount() {
        if(commentList == null)
            return 0;
        return commentList.size();
    }
}
