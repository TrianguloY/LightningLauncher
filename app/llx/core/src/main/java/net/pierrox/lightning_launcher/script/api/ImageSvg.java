package net.pierrox.lightning_launcher.script.api;

import android.graphics.drawable.Drawable;

import net.pierrox.android.lsvg.SvgDrawable;
import net.pierrox.lightning_launcher.script.api.svg.SvgSvg;

/**
 * An image backed by a SVG document
 * <p>
 * An instance of this object can be retrieved with any function that returns an {@link Image} when that image is an ImageSvg.
 */

public class ImageSvg extends Image {
    private final SvgDrawable mSvgDrawable;

    /**
     * @hide
     */
    public ImageSvg(Lightning lightning, SvgDrawable svgDrawable) {
        super(lightning);
        mSvgDrawable = svgDrawable;
    }

    @Override
    public int getWidth() {
        return mSvgDrawable.getIntrinsicWidth();
    }

    @Override
    public int getHeight() {
        return mSvgDrawable.getIntrinsicHeight();
    }

    /**
     * @hide
     */
    @Override
    public Drawable toDrawable() {
        return mSvgDrawable;
    }

    public SvgSvg getSvgRoot() {
        return new SvgSvg(mSvgDrawable.getSvgRoot());
    }
}
