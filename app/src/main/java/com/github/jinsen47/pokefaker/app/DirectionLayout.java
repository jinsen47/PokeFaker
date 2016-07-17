package com.github.jinsen47.pokefaker.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.github.jinsen47.pokefaker.R;

/**
 * Created by Jinsen on 16/7/13.
 */
public class DirectionLayout extends RelativeLayout implements View.OnClickListener{
    private View mContentView;
    private ImageButton mLeft;
    private ImageButton mRight;
    private ImageButton mUp;
    private ImageButton mDown;
    private onDirectionListener mListener;

    public DirectionLayout(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        mContentView = inflater.inflate(R.layout.layout_direction, this);

        mLeft = ((ImageButton) mContentView.findViewById(R.id.left));
        mRight = ((ImageButton) mContentView.findViewById(R.id.right));
        mUp = ((ImageButton) mContentView.findViewById(R.id.up));
        mDown = ((ImageButton) mContentView.findViewById(R.id.down));

        mLeft.setOnClickListener(this);
        mRight.setOnClickListener(this);
        mUp.setOnClickListener(this);
        mDown.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.left:
                if (mListener != null) {
                    mListener.onLeft();
                }
                break;
            case R.id.right:
                if (mListener != null) {
                    mListener.onRight();
                }
                break;
            case R.id.up:
                if (mListener != null) {
                    mListener.onUp();
                }
                break;
            case R.id.down:
                if (mListener != null) {
                    mListener.onDown();
                }
                break;
            default:
                break;
        }
    }

    public void setOnDirectionLisener(onDirectionListener lisener) {
        mListener = lisener;
    }

    public interface onDirectionListener {
        void onUp();
        void onDown();
        void onLeft();
        void onRight();
    }

}
