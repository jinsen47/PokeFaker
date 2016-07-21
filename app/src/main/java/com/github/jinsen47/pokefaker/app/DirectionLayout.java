package com.github.jinsen47.pokefaker.app;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

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
    private double angle;

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
//        rocker.setRefreshCycle(1500);

        if (null != rocker){
            rocker.setListener(new RockerView.RockerListener() {
                @Override
                public void callback(int eventType, int currentAngle, double power) {
                    Log.d("DirectionLayout", "angle: " + currentAngle + ", " + "power: " + power);
                    if(currentAngle > 0){
                        angle = currentAngle* Math.PI / 180;
                        mListener.onDirection(angle, power);
                    }
                    switch (eventType) {
                        case RockerView.EVENT_ACTION:
//                            rocker.setRefreshCycle(1500);
                            break;
                        case RockerView.EVENT_CLOCK:
                            if(currentAngle<0){
//                                rocker.setRefreshCycle(99999);
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
        void onDirection(double angle, double power);
    }

    public interface ServiceListener{
        void OnCloseService();
    }

}
