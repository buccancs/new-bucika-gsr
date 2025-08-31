package com.topdon.module.user.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import com.topdon.module.user.bean.ColorsBean;
import java.util.List;

public class ProgressBarView extends View {
    private Paint paint;
    private int totalParts = 100;
    private List<ColorsBean> colorsBeanList;
    public ProgressBarView(Context context) {
        super(context);
        init();
    }

    public ProgressBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProgressBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        float partWidth = (float) width / totalParts;
        RectF rect = new RectF(0, 0, width, height);
        paint.setColor(Color.parseColor("#00000000"));
        canvas.drawRoundRect(rect, 6f,6f,paint);
        if (colorsBeanList != null ){
            for (int i = 0; i < colorsBeanList.size(); i++) {
                ColorsBean bean = colorsBeanList.get(i);
                paint.setColor(bean.getColor());
                RectF redRect = new RectF(bean.getStart() * partWidth, 0,
                        bean.getEnd() * partWidth, height);
                canvas.drawRect(redRect, paint);
            }
        }
    }

    public void setSegmentPart(List<ColorsBean> colorsBeans) {
        this.colorsBeanList = colorsBeans;
        invalidate();
    }
}
