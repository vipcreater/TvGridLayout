package com.owen.tvgridlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ZhouSuQiang
 * @email zhousuqiang@126.com
 * @date 2018/12/17
 */
public class TvGridLayout extends GridLayout implements View.OnFocusChangeListener, View.OnClickListener {
    private static final int NO_POSITION = -1;

    private Adapter mAdapter;
    private OnItemListener mOnItemListener;
    private AdapterDataObserver mAdapterDataObserver;

    private int mSelectedPosition = NO_POSITION;
    private int mVerticalSpacing;
    private int mHorizontalSpacing;

    TvGridLayout(Context context, AttributeSet attrs, boolean initSuperAttrs) {
        super(context);
        init(attrs, initSuperAttrs);
    }

    public TvGridLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, false);
    }

    public TvGridLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TvGridLayout(Context context) {
        this(context, null, 0);
    }

    private void init(AttributeSet attrs, boolean initSuperAttrs) {
        setClipToPadding(false);
        setClipChildren(false);
        setFocusable(true);
        setChildrenDrawingOrderEnabled(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);

        if (null != attrs) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.TvGridLayout);
            try {
                int vs = typedArray.getDimensionPixelOffset(R.styleable.TvGridLayout_verticalSpacing, 0);
                int hs = typedArray.getDimensionPixelOffset(R.styleable.TvGridLayout_horizontalSpacing, 0);
                setItemSpacing(vs, hs);
                if (initSuperAttrs) {
                    this.setRowCount(typedArray.getInt(R.styleable.TvGridLayout_rowCount, -2147483648));
                    this.setColumnCount(typedArray.getInt(R.styleable.TvGridLayout_columnCount, -2147483648));
                    this.setOrientation(typedArray.getInt(R.styleable.TvGridLayout_orientation, 0));
                    this.setUseDefaultMargins(typedArray.getBoolean(R.styleable.TvGridLayout_useDefaultMargins, false));
                    this.setAlignmentMode(typedArray.getInt(R.styleable.TvGridLayout_alignmentMode, 1));
                    this.setRowOrderPreserved(typedArray.getBoolean(R.styleable.TvGridLayout_rowOrderPreserved, true));
                    this.setColumnOrderPreserved(typedArray.getBoolean(R.styleable.TvGridLayout_columnOrderPreserved, true));
                }
            } finally {
                typedArray.recycle();
            }
        }

        mAdapterDataObserver = new AdapterDataObserver() {
            @Override
            public void onAdapterDataChanged() {
                TvGridLayout.this.onAdapterDataChanged();
            }
        };
    }

    @Override
    public boolean isInEditMode() {
        return true;
    }

    @Override
    public boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return super.checkLayoutParams(p);
    }

    @Override
    public LayoutParams generateDefaultLayoutParams() {
        return super.generateDefaultLayoutParams();
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        //焦点记忆
        final int position = mSelectedPosition == NO_POSITION ? 0 : mSelectedPosition;
        View child = getChildAt(position);
        if (null != child) {
            if(null != getOnFocusChangeListener()) {
                getOnFocusChangeListener().onFocusChange(this, true);
            }
            return child.requestFocus(direction, previouslyFocusedRect);
        }
        return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (!hasFocus() && isFocusable()) {
            views.add(this);
            return;
        }
        super.addFocusables(views, direction, focusableMode);
    }

    @Override
    public void onViewAdded(View child) {
        if (!getUseDefaultMargins()) {
            //行列间距
            int vs = mVerticalSpacing / 2;
            int hs = mHorizontalSpacing / 2;
            LayoutParams params = (LayoutParams) child.getLayoutParams();
//            int ml = params.leftMargin + hs;
//            int mt = params.topMargin + vs;
//            int mr = params.rightMargin + hs;
//            int mb = params.bottomMargin + vs;
//            params.setMargins(ml, mt, mr, mb);
            params.setMargins(hs, vs, hs, vs);
        }

        //焦点和点击监听
        if (null == child.getOnFocusChangeListener()) {
            child.setOnFocusChangeListener(this);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 || !child.hasOnClickListeners()) {
            child.setOnClickListener(this);
        }
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        postInvalidate();
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        View focusedView = getFocusedChild();
        if (null != focusedView) {
            int position = indexOfChild(focusedView);
            if (i == childCount - 1) {
                //这是最后一个需要刷新的item
                if (position > i) {
                    position = i;
                }
                return position;
            } else if (position == i) {
                //这是原本要在最后一个刷新的item
                return childCount - 1;
            }
        }
        return i;
    }

    @Override
    public void onFocusChange(View child, boolean hasFocus) {
        int position = indexOfChild(child);
        if (hasFocus) {
            removeCallbacks(mLostFocusRunnable);
            mSelectedPosition = position;
            if (null != mOnItemListener) {
                mOnItemListener.onItemSelected(this, child, position);
            }
        } else {
            if (null != mOnItemListener) {
                mOnItemListener.onItemPreSelected(this, child, position);
            }
            postDelayed(mLostFocusRunnable, 200);
        }
    }

    private Runnable mLostFocusRunnable = new Runnable() {
        @Override
        public void run() {
            if(null != getOnFocusChangeListener()) {
                getOnFocusChangeListener().onFocusChange(TvGridLayout.this, false);
            }
        }
    };

    @Override
    public void onClick(View child) {
        if (null != mOnItemListener) {
            int position = indexOfChild(child);
            mOnItemListener.onItemClick(this, child, position);
        }
    }

    /**
     * 设置item行列间距
     *
     * @param verticalSpacing
     * @param horizontalSpacing
     */
    public void setItemSpacing(int verticalSpacing, int horizontalSpacing) {
        int oldVs = mVerticalSpacing / 2;
        int oldHs = mHorizontalSpacing / 2;

        this.mVerticalSpacing = verticalSpacing;
        this.mHorizontalSpacing = horizontalSpacing;

        //修正padding
        int pl = getPaddingLeft() + oldHs;
        int pt = getPaddingTop() + oldVs;
        int pr = getPaddingRight() + oldHs;
        int pb = getPaddingBottom() + oldVs;
        setPadding(pl, pt, pr, pb);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        int newVs = mVerticalSpacing / 2;
        int newHs = mHorizontalSpacing / 2;
        super.setPadding(left - newHs, top - newVs, right - newHs, bottom - newVs);
    }

    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    public void setOnItemListener(OnItemListener onItemListener) {
        mOnItemListener = onItemListener;
    }

    public OnItemListener getOnItemListener() {
        return mOnItemListener;
    }

    public void setAdapter(Adapter adapter) {
        if (null != mAdapter) {
            mAdapter.unregisterAdapterDataObserver(mAdapterDataObserver);
        }
        this.mAdapter = adapter;
        if (null != this.mAdapter) {
            this.mAdapter.registerAdapterDataObserver(mAdapterDataObserver);
            onAdapterDataChanged();
        }
    }

    public void onAdapterDataChanged() {
        removeAllViews();
        if (null != mAdapter && mAdapter.getItemCount() > 0) {
            for (int i = 0; i < mAdapter.getItemCount(); i++) {
                ViewHolder holder = mAdapter.onCreateViewHolder(i, getContext(), this);
                addView(holder.itemView, holder.createLayoutParams());
            }
        }
    }

    public interface OnItemListener {
        void onItemPreSelected(TvGridLayout parent, View itemView, int position);

        void onItemSelected(TvGridLayout parent, View itemView, int position);

        void onItemClick(TvGridLayout parent, View itemView, int position);
    }

    public abstract class AdapterDataObserver {
        public abstract void onAdapterDataChanged();
    }

    public static class ViewHolder {

        public int gravity = 0;
        public int column = -2147483648;
        public int colSpan = 1;
        public float colWeight = 0.0F;
        public int row = -2147483648;
        public int rowSpan = 1;
        public float rowWeight = 0.0F;

        public View itemView;

        LayoutParams createLayoutParams() {
            Spec colSpec = GridLayout.spec(column, colSpan, colWeight);
            Spec rowSpec = GridLayout.spec(row, rowSpan, rowWeight);
            LayoutParams params = new LayoutParams(rowSpec, colSpec);
            params.setGravity(gravity);
            params.width = itemView.getLayoutParams().width;
            params.height = itemView.getLayoutParams().height;
            return params;
        }

        public <T extends View> T getView(int viewId) {
            return null != itemView ? (T)itemView.findViewById(viewId) : null;
        }
    }

    public static abstract class Adapter<T> {
        private List<AdapterDataObserver> mObservers = new ArrayList<>();
        private List<T> mDatas;

        public Adapter() {

        }

        public Adapter(List<T> datas) {
            setDatas(datas);
        }

        public void setDatas(List<T> datas) {
            this.mDatas = datas;
        }

        public void appendDatas(List<T> datas) {
            if (null != datas) {
                this.mDatas.addAll(datas);
            }
        }

        ViewHolder onCreateViewHolder(int position, Context context, ViewGroup parent) {
            ViewHolder holder = new ViewHolder();
            int layout = getItemLayout(position, getItemViewType(position));
            holder.itemView = LayoutInflater.from(context).inflate(layout, parent, false);
            onBindViewHolder(holder, getItem(position), position, getItemViewType(position));
            return holder;
        }

        public int getItemViewType(int position) {
            return 0;
        }

        public T getItem(int position) {
            return null != mDatas ? mDatas.get(position) : null;
        }

        public void notifyDataSetChanged() {
            for (AdapterDataObserver observer : mObservers) {
                observer.onAdapterDataChanged();
            }
        }

        public void registerAdapterDataObserver(@NonNull AdapterDataObserver observer) {
            mObservers.add(observer);
        }

        public void unregisterAdapterDataObserver(@NonNull AdapterDataObserver observer) {
            mObservers.remove(observer);
        }

        public abstract int getItemCount();

        public abstract int getItemLayout(int position, int viewType);

        public abstract void onBindViewHolder(@NonNull ViewHolder holder, T item, int position, int viewType);
    }
}