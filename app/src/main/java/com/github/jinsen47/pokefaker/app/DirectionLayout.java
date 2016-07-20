package com.github.jinsen47.pokefaker.app;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gcssloop.widget.RockerView;
import com.github.jinsen47.pokefaker.R;

/**
 * Created by Jinsen on 16/7/13.
 */
public class DirectionLayout extends RelativeLayout {
    private Context mContext;
    private View mContentView;
    private ServiceListener mSerLisrener;
    private RockerView rocker;
    private onDirectionListener mListener;
    private double agle;

    public DirectionLayout(Context context, ServiceListener listener) {
        super(context);
        mContext = context;
        mSerLisrener = listener;
        init();
    }

    private void init() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mContentView = inflater.inflate(R.layout.layout_direction, this);

        rocker = (RockerView) mContentView.findViewById(R.id.rocker);
        rocker.setRefreshCycle(99999);
//        ((ImageView)mContentView.findViewById(R.id.imageView)).setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(mSerLisrener!=null)
//                    mSerLisrener.OnCloseService();
//            }
//        });

        if (null != rocker){
            rocker.setListener(new RockerView.RockerListener() {
                @Override
                public void callback(int eventType, int currentAngle) {
                    if(currentAngle>0){
                        agle = currentAngle* Math.PI / 180;
                        mListener.onDirection(agle);
                    }
                    switch (eventType) {
                        case RockerView.EVENT_ACTION:
                            rocker.setRefreshCycle(1500);
                            break;
                        case RockerView.EVENT_CLOCK:
                            if(currentAngle<0){
                                rocker.setRefreshCycle(99999);
                            }
                            break;
                    }
                }
            });
        }
    }

    public void setOnDirectionLisener(onDirectionListener lisener) {
        mListener = lisener;
    }

    public interface onDirectionListener {
        void onDirection(double agle);
    }

    public interface ServiceListener{
        void OnCloseService();
    }

}
