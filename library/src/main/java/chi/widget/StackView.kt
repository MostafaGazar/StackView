package chi.widget

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.database.DataSetObserver
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Adapter
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import chi.widget.animation.AnimatorListener
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val KEY_SUPER_STATE = "superState"
private const val KEY_CURRENT_INDEX = "currentIndex"

private const val DEFAULT_STACK_MAX_SIZE = 3
private const val DEFAULT_ANIMATION_DURATION = 300 * 1
private const val DEFAULT_SWIPE_ROTATION = 20F
private const val DEFAULT_SWIPE_OPACITY = 1F
private const val DEFAULT_ENABLE_ELEVATION = true

class StackView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    var adapter: Adapter? = null
        set(value) {
            field?.unregisterDataSetObserver(dataObserver)
            field?.registerDataSetObserver(dataObserver)

            field = value
        }

    var onChangeListener: OnChangeListener? = null

    private var stackMaxSize = DEFAULT_STACK_MAX_SIZE
    private var viewSpacing = 0
    private var animationDuration = DEFAULT_ANIMATION_DURATION
    private var swipeRotation = DEFAULT_SWIPE_ROTATION
    private var swipeOpacity = DEFAULT_SWIPE_OPACITY
    private var enableElevation = DEFAULT_ENABLE_ELEVATION

    private var currentIndex = 0
    private val topViewIndex
        get() = childCount - 1

    private var previousTouchX = 0F
    private var previousTouchY = 0F
    private var initialX = 0F
    private var initialY = 0F
    private var touchPointerId = 0

    private val originalAllParentsClipChildrenConfig = mutableMapOf<Int, Boolean>()
    private val originalAllParentsClipToPaddingConfig = mutableMapOf<Int, Boolean>()

    private val dataObserver: DataSetObserver = object : DataSetObserver() {
        override fun onChanged() {
            super.onChanged()

            invalidate()
            requestLayout()
        }
    }

    init {
        processAttributes(attrs)

        initializeViews()
    }

    private fun processAttributes(attrs: AttributeSet?) {
        attrs?.let {
            val attributeArray = context.obtainStyledAttributes(attrs, R.styleable.StackView)

            try {
                stackMaxSize = attributeArray.getInt(R.styleable.StackView_stack_max_size, DEFAULT_STACK_MAX_SIZE)
                viewSpacing = attributeArray.getDimensionPixelSize(
                    R.styleable.StackView_stack_spacing,
                    resources.getDimensionPixelSize(R.dimen.default_stack_spacing)
                )
                animationDuration = attributeArray.getInt(
                    R.styleable.StackView_stack_animation_duration,
                    DEFAULT_ANIMATION_DURATION
                )
                swipeRotation = attributeArray.getFloat(
                    R.styleable.StackView_stack_swipe_rotation,
                    DEFAULT_SWIPE_ROTATION
                )
                swipeOpacity = attributeArray.getFloat(
                    R.styleable.StackView_stack_swipe_opacity,
                    DEFAULT_SWIPE_OPACITY
                )
                enableElevation = attributeArray.getBoolean(
                    R.styleable.StackView_stack_enable_elevation,
                    DEFAULT_ENABLE_ELEVATION
                )
            } finally {
                attributeArray.recycle()
            }
        }
    }

    private fun initializeViews() {
        clipToPadding = false
        clipChildren = false

        isScrollContainer = false
        isFocusableInTouchMode = true
    }

    @CallSuper
    override fun onSaveInstanceState(): Parcelable? = Bundle().apply {
        putInt(KEY_CURRENT_INDEX, currentIndex)
        putParcelable(KEY_SUPER_STATE, super.onSaveInstanceState())
    }

    @CallSuper
    override fun onRestoreInstanceState(state: Parcelable?) {
        var viewState = state
        if (viewState is Bundle) {
            currentIndex = viewState.getInt(KEY_CURRENT_INDEX)
            viewState = viewState.getParcelable(KEY_SUPER_STATE)
        }

        super.onRestoreInstanceState(viewState)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (adapter == null || adapter?.isEmpty == true) {
            currentIndex = 0
            removeAllViewsInLayout()

            return
        }

        adapter?.let { adapter ->
            val stackSize = min(adapter.count - currentIndex, stackMaxSize)
            val diff = stackSize - childCount
            val startIndexToAdd = currentIndex + childCount

            val initialOrder = childCount
            for (i in 0 until diff) {
                val index = stackSize - i - 1

                addItem(adapter.getView(startIndexToAdd + i, null, this), index, initialOrder + i)
            }
        }

        reorderItems()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!isEnabled) {
                    return false
                }

                requestDisallowInterceptTouchEvent(true)

                val childView = getChildAt(topViewIndex)
                initialX = childView.x
                initialY = childView.y

                touchPointerId = event.getPointerId(0)
                previousTouchX = event.getX(touchPointerId)
                previousTouchY = event.getY(touchPointerId)

                setAllParentsClipConfig(enabled = false)

                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(touchPointerId)
                if (pointerIndex < 0) {
                    return false
                }

                val childView = getChildAt(topViewIndex)
                val touchX = event.getX(pointerIndex)
                val touchY = event.getY(pointerIndex)
                val newX = childView.x + (touchX - previousTouchX)
                val newY = childView.y + (touchY - previousTouchY)

                previousTouchX = touchX
                previousTouchY = touchY
                childView.x = newX
                childView.y = newY

                val dragDistanceX = newX - initialX
                val swipeProgress = max(
                    dragDistanceX / width, -1F
                ).coerceAtMost(1F)

                if (swipeRotation > 0) {
                    childView.rotation = swipeRotation * swipeProgress
                }

                if (swipeOpacity < 1f) {
                    childView.alpha = 1F - abs(swipeProgress * 2).coerceAtMost(1F)
                }

                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->{
                requestDisallowInterceptTouchEvent(false)

                swipeOrResetViewPosition()

                return true
            }
        }

        return super.onTouchEvent(event)
    }

    fun next() {
        val rtl = resources.getBoolean(R.bool.is_rtl)
        if (rtl) {
            swipeItemToLeft()
        } else {
            swipeItemToRight()
        }
    }

    private fun addItem(childView: View, index: Int, order: Int) {
        val width = width - (paddingStart + paddingEnd)
        val height = height - (paddingTop + paddingBottom)

        // Measure child view
        var params = childView.layoutParams
        if (params == null) {
            params = LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val measureSpecWidth = calculateMeasureSpec(params.width)
        val measureSpecHeight = calculateMeasureSpec(params.height)
        childView.measure(measureSpecWidth or width, measureSpecHeight or height)

        // Layout child view
        layoutItem(childView, index, order)
        scaleItem(childView, order)

        // Add child view
        addViewInLayout(childView, 0, params, true)
    }

    private fun reorderItems(animate: Boolean = true) {
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            val order = childCount - i - 1

            val layoutViewAnimationSet = layoutItem(childView, i, order, allowAnimation = animate)
            val scaleViewAnimationSet = scaleItem(childView, order, allowAnimation = animate)

            if (animate) {
                val animatorSet = AnimatorSet().apply {
                    interpolator = OvershootInterpolator()
                    duration = animationDuration / 2L

                    playTogether(layoutViewAnimationSet, scaleViewAnimationSet)
                }
                animatorSet.start()
            }
        }
    }

    private fun layoutItem(childView: View, index: Int, order: Int, allowAnimation: Boolean = false): AnimatorSet? {
        val newPositionX = (width - childView.measuredWidth) / 2
        childView.layout(
            newPositionX,
            paddingTop,
            newPositionX + childView.measuredWidth,
            paddingTop + childView.measuredHeight
        )

        if (enableElevation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            childView.translationZ = index.toFloat()
        }

        val distanceToViewAbove = order * viewSpacing
        val newPositionY = distanceToViewAbove + paddingTop
        return if (allowAnimation) {
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(childView, "y", newPositionY.toFloat())
                )
            }
        } else {
            childView.y = newPositionY.toFloat()

            null
        }
    }

    private fun scaleItem(childView: View, order: Int, allowAnimation: Boolean = false): AnimatorSet? {
        val scaleFactor = 1F - order * .05F

        return if (allowAnimation) {
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(childView, "scaleX", scaleFactor),
                    ObjectAnimator.ofFloat(childView, "scaleY", scaleFactor)
                )
            }
        } else {
            childView.scaleY = scaleFactor
            childView.scaleX = scaleFactor

            null
        }
    }

    private fun swipeOrResetViewPosition() {
        if (!isEnabled) {
            resetItemPosition()
            return
        }

        val childView = getChildAt(topViewIndex)
        val threshold = width / 3F
        when {
            childView.x + childView.width < 2 * threshold -> {
                swipeItemToLeft()
            }
            childView.x > threshold -> {
                swipeItemToRight()
            }
            else -> {
                resetItemPosition()
            }
        }
    }

    private fun resetItemPosition() {
        val childView = getChildAt(topViewIndex)

        childView.animate()
            .x(initialX)
            .y(initialY)
            .rotation(0F)
            .alpha(1F)
            .setDuration(animationDuration.toLong())
            .setInterpolator(OvershootInterpolator(1.4F))
            .setListener(object: AnimatorListener() {
                override fun onAnimationEnd(animation: Animator?) {
                    resetAllParentsClipConfig()
                }

                override fun onAnimationCancel(animation: Animator) {
                    resetAllParentsClipConfig()
                }
            })
    }

    private fun swipeItemToLeft() {
        val chileView = getChildAt(topViewIndex)

        chileView.animate().cancel()
        // TODO :: Control y as well via fling for a smooth transition between user dragging the item and the animation
        chileView.animate()
            .x(-width + chileView.x)
            .rotation(-swipeRotation)
//            .alpha(0f)
            .setDuration(animationDuration.toLong())
            .setInterpolator(LinearOutSlowInInterpolator())
            .setListener(object: AnimatorListener() {
                override fun onAnimationEnd(animation: Animator?) {
                    removeTopItem()

                    resetAllParentsClipConfig()
//                    onViewSwipedToLeft()
                }

                override fun onAnimationCancel(animation: Animator) {
                    resetAllParentsClipConfig()
                }
            })
    }

    private fun swipeItemToRight() {
        val childView = getChildAt(topViewIndex)

        childView.animate().cancel()
        // TODO :: Control y as well via fling for a smooth transition between user dragging the item and the animation
        childView.animate()
            .x(width + childView.x)
            .rotation(swipeRotation)
//            .alpha(0f)
            .setDuration(animationDuration.toLong())
            .setInterpolator(LinearOutSlowInInterpolator())
            .setListener(object: AnimatorListener() {
                override fun onAnimationEnd(animation: Animator?) {
                    removeTopItem()

                    resetAllParentsClipConfig()
//                    onViewSwipedToRight()
                }

                override fun onAnimationCancel(animation: Animator) {
                    resetAllParentsClipConfig()
                }
            })
    }

    private fun removeTopItem() {
        if (childCount > 0) {
            removeViewAt(topViewIndex)
        }

        currentIndex += 1

        adapter?.let { adapter ->
            onChangeListener?.onChange(adapter.count - currentIndex, adapter.count)
        }
    }

    private fun setAllParentsClipConfig(enabled: Boolean = false) {
        var view: View = this

        while (view.parent != null && view.parent is ViewGroup) {
            val viewGroup = view.parent as ViewGroup

            originalAllParentsClipChildrenConfig[viewGroup.id] = viewGroup.clipChildren
            originalAllParentsClipToPaddingConfig[viewGroup.id] = viewGroup.clipToPadding

            viewGroup.clipChildren = enabled
            viewGroup.clipToPadding = enabled

            view = viewGroup
        }
    }

    private fun resetAllParentsClipConfig() {
        var view: View = this

        while (view.parent != null && view.parent is ViewGroup) {
            val viewGroup = view.parent as ViewGroup

            viewGroup.clipChildren = originalAllParentsClipChildrenConfig[viewGroup.id] ?: true
            viewGroup.clipToPadding = originalAllParentsClipToPaddingConfig[viewGroup.id] ?: true

            view = viewGroup
        }

        originalAllParentsClipChildrenConfig.clear()
        originalAllParentsClipToPaddingConfig.clear()
    }

    private fun calculateMeasureSpec(param: Int) = if (param == LayoutParams.MATCH_PARENT) {
        MeasureSpec.EXACTLY
    } else {
        MeasureSpec.AT_MOST
    }

}