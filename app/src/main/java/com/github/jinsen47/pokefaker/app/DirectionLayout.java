package com.github.jinsen47.pokefaker.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.gcssloop.widget.RockerView;
import com.github.jinsen47.pokefaker.R;

/**
 * Created by Jinsen on 16/7/13.
 */
public class DirectionLayout extends RelativeLayout {
    private Context mContext;
    private View mContentView;
    private RockerView rocker;
    private ImageView moveInfo;
    private ImageView moveButton;
    private onDirectionLayoutListener mListener;
    private WindowManager windowManager;
    private double radian = 0D;
    private float xInScreen;
    private float yInScreen;
    private boolean isMovingView;


    public DirectionLayout(Context context) {
        super(context);
        mContext = context;
        windowManager=(WindowManager)getContext().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        init();
    }

    private void init() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mContentView = inflater.inflate(R.layout.layout_direction, this);
        rocker = (RockerView) mContentView.findViewById(R.id.rocker);
        rocker.setRefreshCycle(99999);
        moveInfo = ((ImageView)mContentView.findViewById(R.id.move_info));
        moveButton = ((ImageView)mContentView.findViewById(R.id.arrowMove));
        moveButton.setVisibility(INVISIBLE);
        moveInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isMovingView){
                    moveInfo.setBackgroundResource(R.drawable.move_info);
                    setRockerEnabled(true);
                }else{
                    moveInfo.setBackgroundResource(R.drawable.close_popw);
                    setRockerEnabled(false);
                }
                isMovingView = !isMovingView;
            }
        });

        if (null != rocker){
            rocker.setListener(new RockerView.RockerListener() {
                @Override
                public void callback(int eventType, int currentAngle, double power) {
                    if (currentAngle > 0){
                        radian = currentAngle * Math.PI / 180;
                        mListener.onDirection(radian, power);
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

    public void setRockerEnabled(boolean flag){
        if(!flag){
            rocker.setAreaColor(0x99777777);
            rocker.setRockerColor(0x00FFFFFF);
            moveButton.setVisibility(VISIBLE);
            moveButton.setOnTouchListener(mTouchListener);
            mListener.getLayoutParams().alpha=1f;
        }else{
            rocker.setAreaColor(0x44777777);
            rocker.setRockerColor(0x77555555);
            moveButton.setVisibility(INVISIBLE);
            moveButton.setOnTouchListener(null);
            mListener.getLayoutParams().alpha=0.5f;
        }
        windowManager.updateViewLayout(this, mListener.getLayoutParams());
        rocker.setEnabled(flag);
    }

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    xInScreen = event.getRawX();
                    yInScreen = event.getRawY();
                    updateViewPosition();
                    break;
                default:
                    break;
            }
            return true;
        }
    };

    private void updateViewPosition(){
        mListener.getLayoutParams().x = (int) (xInScreen-this.getWidth()/2);
        mListener.getLayoutParams().y = (int) (yInScreen-this.getHeight()/2);
        windowManager.updateViewLayout(this, mListener.getLayoutParams());
        saveWindowPosition(mListener.getLayoutParams().x, mListener.getLayoutParams().y);
    }

    private void saveWindowPosition(int x, int y) {
        SharedPreferences mSp = getContext().getSharedPreferences("default", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSp.edit();
        editor.putInt("window_x", x);
        editor.putInt("window_y", y);
        editor.apply();
    }

    public interface onDirectionLayoutListener {
        void onDirection(double radian, double power);
        WindowManager.LayoutParams getLayoutParams();
    }

    public void setDirectionLayoutListener(onDirectionLayoutListener listener) {
        mListener = listener;
    }

}
