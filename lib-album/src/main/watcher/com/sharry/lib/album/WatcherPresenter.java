package com.sharry.lib.album;

import android.os.Handler;
import android.os.Looper;

import java.text.MessageFormat;
import java.util.ArrayList;

/**
 * The presenter associated with PictureWatcher.
 *
 * @author Sharry <a href="SharryChooCHN@Gmail.com">Contact me.</a>
 * @version 1.0
 * @since 2019/3/15 21:56
 */
class WatcherPresenter implements WatcherContract.IPresenter {

    /**
     * Final fields.
     */
    private final WatcherContract.IView mView;
    private final WatcherConfig mConfig;
    private final ArrayList<MediaMeta> mDisplayMetas;
    private final ArrayList<MediaMeta> mPickedSet;
    private final SharedElementHelper.Bounds mSharedElementEnterData;
    private int mCurPosition;
    private MediaMeta mCurDisplay;

    WatcherPresenter(WatcherContract.IView view, WatcherConfig config, SharedElementHelper.Bounds sharedElementModel) {
        this.mView = view;
        this.mConfig = config;
        this.mSharedElementEnterData = sharedElementModel;
        // 获取需要展示图片的 URI 集合
        this.mDisplayMetas = config.getPictureUris();
        // 获取已经选中的图片
        this.mPickedSet = config.getUserPickedSet();
        // 获取当前需要展示的 Position 和 URI
        this.mCurPosition = config.getPosition();
        this.mCurDisplay = mDisplayMetas.get(mCurPosition);
        // 配置视图
        setupViews();
    }

    private void setupViews() {
        // 1. 设置 Toolbar 数据
        mView.setLeftTitleText(buildToolbarLeftText());
        mView.setIndicatorVisible(mConfig.isPickerSupport());

        // 2. 设置 Pictures 数据
        mView.setDisplayAdapter(mDisplayMetas);
        mView.displayAt(mCurPosition);

        // 3. 设置底部菜单和按钮选中的状态
        if (mConfig.isPickerSupport()) {
            mView.setIndicatorColors(
                    mConfig.getIndicatorBorderCheckedColor(),
                    mConfig.getIndicatorBorderUncheckedColor(),
                    mConfig.getIndicatorSolidColor(),
                    mConfig.getIndicatorTextColor()
            );
            mView.setIndicatorChecked(mPickedSet.indexOf(mCurDisplay) != -1);
            mView.setIndicatorText(buildToolbarCheckedIndicatorText());
            // 底部菜单
            mView.setPickedAdapter(mPickedSet);
            mView.setEnsureText(buildEnsureText());
            // 底部菜单延时弹出
            if (!mPickedSet.isEmpty()) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mView.showPickedPanel();
                    }
                }, mSharedElementEnterData != null ? 500 : 0);
            }
        }

        // 4. 执行共享元素入场动画
        if (mSharedElementEnterData != null) {
            mView.showSharedElementEnter(mDisplayMetas.get(mCurPosition), mSharedElementEnterData);
        }
    }

    @Override
    public void handlePagerChanged(int position) {
        // 更新数据
        mCurPosition = position;
        mCurDisplay = mDisplayMetas.get(position);
        // 展示 Toolbar 左边的指示文本
        mView.setLeftTitleText(buildToolbarLeftText());
        // 展示图片
        mView.displayAt(mCurPosition);
        if (mConfig.isPickerSupport()) {
            mView.setIndicatorChecked(mPickedSet.indexOf(mCurDisplay) != -1);
            mView.setIndicatorText(buildToolbarCheckedIndicatorText());
            mView.setEnsureText(buildEnsureText());
        }
    }

    @Override
    public void handleIndicatorClick(boolean isChecked) {
        if (isChecked) {
            // 移除选中数据与状态
            int removedIndex = mPickedSet.indexOf(mCurDisplay);
            if (removedIndex < 0) {
                return;
            }
            mPickedSet.remove(removedIndex);
            // 通知 RecyclerView 数据变更
            mView.notifyItemRemoved(mCurDisplay, removedIndex);
        } else {
            // 判断是否达到选择上限
            if (mPickedSet.size() < mConfig.getThreshold()) {
                mPickedSet.add(mCurDisplay);
                int addedIndex = mPickedSet.indexOf(mCurDisplay);
                // 通知 RecyclerView 数据变更
                mView.notifyItemPicked(mCurDisplay, addedIndex);
                mView.pickedPanelSmoothScrollToPosition(addedIndex);
            } else {
                mView.showMsg(
                        mView.getString(R.string.lib_album_watcher_tips_over_threshold_prefix) +
                                mConfig.getThreshold() +
                                mView.getString(R.string.lib_album_watcher_tips_over_threshold_suffix)
                );
            }
        }
        mView.setIndicatorChecked(mPickedSet.indexOf(mCurDisplay) != -1);
        mView.setIndicatorText(buildToolbarCheckedIndicatorText());
        mView.setEnsureText(buildEnsureText());
        // 控制底部导航栏的展示
        if (mPickedSet.isEmpty()) {
            mView.dismissPickedPanel();
        } else {
            mView.showPickedPanel();
        }
    }

    @Override
    public void handlePickedItemClicked(MediaMeta meta) {
        int index = mDisplayMetas.indexOf(meta);
        if (index >= 0) {
            handlePagerChanged(index);
        }
    }

    @Override
    public void handleEnsureClicked() {
        if (mPickedSet.isEmpty()) {
            mView.showMsg(mView.getString(R.string.lib_album_watcher_tips_ensure_failed));
            return;
        }
        mView.sendEnsureBroadcast();
        mView.finish();
    }

    @Override
    public boolean handleDisplayPagerDismiss() {
        // 尝试获取退出时共享元素的数据
        SharedElementHelper.Bounds exitData = getExitSharedElement();
        // 若存在则消费这个 dismiss 事件
        if (exitData != null) {
            mView.showSharedElementExitAndFinish(exitData);
            mView.dismissPickedPanel();
            return true;
        }
        return false;
    }

    @Override
    public SharedElementHelper.Bounds getExitSharedElement() {
        if (mSharedElementEnterData == null) {
            return null;
        }
        return mSharedElementEnterData.position == mCurPosition ?
                mSharedElementEnterData : SharedElementHelper.CACHES.get(mCurPosition);
    }

    /**
     * 构建 Toolbar 左边的文本
     */
    private CharSequence buildToolbarLeftText() {
        return MessageFormat.format("{0}/{1}", mCurPosition + 1, mDisplayMetas.size());
    }

    /**
     * 构建 Toolbar checked Indicator 的文本
     */
    private CharSequence buildToolbarCheckedIndicatorText() {
        return String.valueOf(mPickedSet.indexOf(mCurDisplay) + 1);
    }

    /**
     * 构建确认按钮文本
     */
    private CharSequence buildEnsureText() {
        return MessageFormat.format("{0}({1}/{2})",
                mView.getString(R.string.lib_album_watcher_ensure), mPickedSet.size(), mConfig.getThreshold());
    }

}
