package com.butterfly.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class ViewPagerMapBevelScroll extends android.support.v4.view.ViewPager {

    private static final int DEFAULT_SWIPE_MARGIN_WIDTH_DIP = 100;
    private int swipeMarginWidth;
    private static final int MAP_FRAGMENT_INDEX = 2;

    public ViewPagerMapBevelScroll(Context context) {
        super(context);
        setDefaultSwipeMargin(context);
    }

    public ViewPagerMapBevelScroll(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDefaultSwipeMargin(context);
    }

    private void setDefaultSwipeMargin(final Context context) {
        swipeMarginWidth = (int) (DEFAULT_SWIPE_MARGIN_WIDTH_DIP * context
                .getResources().getDisplayMetrics().density);
    }

    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
            if (v instanceof ViewPagerMapBevelScroll) {
            	
                if (getCurrentItem() == MAP_FRAGMENT_INDEX) {
                    return !isAllowedMapSwipe(x, dx);
                }
            }
            return super.canScroll(v, checkV, dx, x, y);
        }

    /**
     * Determines if the pointer movement event at x and moved pixels is
     * considered an allowed swipe movement overriding the inner horizontal
     * scroll content protection.
     * 
     * @param x X coordinate of the active touch point
     * @param dx Delta scrolled in pixels
     * @return true if the movement should start a page swipe
     */
    protected boolean isAllowedMapSwipe(final float x, final float dx) {        
        return ((x < swipeMarginWidth) && (dx > 0))
            || ((x > (getWidth() - swipeMarginWidth)) && (dx < 0));
    }
}