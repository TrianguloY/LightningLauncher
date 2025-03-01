/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.pierrox.lightning_launcher.script.api.palette;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;

import net.pierrox.lightning_launcher.script.ScriptExecutor;

import org.mozilla.javascript.RhinoException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A helper class to extract prominent colors from an image.
 * <p>
 * This is extracted from https://android.googlesource.com/platform/frameworks/support/+/master/v7/palette/src/android/support/v7/graphics.
 * For an example on how to use this class in Lightning Launcher, please have a look at http://www.pierrox.net/android/applications/lightning_launcher/wiki/doku.php?id=script_palette
 *
 * <p>
 * A number of colors with different profiles are extracted from the image:
 * <ul>
 *     <li>Vibrant</li>
 *     <li>Vibrant Dark</li>
 *     <li>Vibrant Light</li>
 *     <li>Muted</li>
 *     <li>Muted Dark</li>
 *     <li>Muted Light</li>
 * </ul>
 * These can be retrieved from the appropriate getter method.
 *
 * <p>
 * An instance of this object can be created with {@link #generate(android.graphics.Bitmap)} or {@link #generate(android.graphics.Bitmap, int)}.
 * <p>
 * These should be called on a background thread, ideally the one in
 * which you load your images on. Sometimes that is not possible, so asynchronous factory methods
 * have also been provided: {@link #generateAsync(android.graphics.Bitmap, net.pierrox.lightning_launcher.script.api.palette.Palette.PaletteAsyncListener)} and
 * {@link #generateAsync(android.graphics.Bitmap, int, net.pierrox.lightning_launcher.script.api.palette.Palette.PaletteAsyncListener)}. These can be used as so:
 *
 * <pre>
 * Palette.generateAsync(bitmap, new Palette.PaletteAsyncListener() {
 *     public void onGenerated(Palette palette) {
 *         // Do something with colors...
 *     }
 * });
 * </pre>
 */
public final class Palette {

    private static final int CALCULATE_BITMAP_MIN_DIMENSION = 100;
    private static final int DEFAULT_CALCULATE_NUMBER_COLORS = 16;
    private static final float TARGET_DARK_LUMA = 0.26f;
    private static final float MAX_DARK_LUMA = 0.45f;
    private static final float MIN_LIGHT_LUMA = 0.55f;
    private static final float TARGET_LIGHT_LUMA = 0.74f;
    private static final float MIN_NORMAL_LUMA = 0.3f;
    private static final float TARGET_NORMAL_LUMA = 0.5f;
    private static final float MAX_NORMAL_LUMA = 0.7f;
    private static final float TARGET_MUTED_SATURATION = 0.3f;
    private static final float MAX_MUTED_SATURATION = 0.4f;
    private static final float TARGET_VIBRANT_SATURATION = 1f;
    private static final float MIN_VIBRANT_SATURATION = 0.35f;
    private final List<Swatch> mSwatches;
    private final int mHighestPopulation;
    private final Swatch mMutedSwatch;
    private final Swatch mDarkMutedSwatch;
    private final Swatch mLightVibrantSwatch;
    private final Swatch mLightMutedColor;
    private Swatch mVibrantSwatch;
    private Swatch mDarkVibrantSwatch;

    private Palette(List<Swatch> swatches) {
        mSwatches = swatches;
        mHighestPopulation = findMaxPopulation();

        mVibrantSwatch = findColor(TARGET_NORMAL_LUMA, MIN_NORMAL_LUMA, MAX_NORMAL_LUMA,
                TARGET_VIBRANT_SATURATION, MIN_VIBRANT_SATURATION, 1f);

        mLightVibrantSwatch = findColor(TARGET_LIGHT_LUMA, MIN_LIGHT_LUMA, 1f,
                TARGET_VIBRANT_SATURATION, MIN_VIBRANT_SATURATION, 1f);

        mDarkVibrantSwatch = findColor(TARGET_DARK_LUMA, 0f, MAX_DARK_LUMA,
                TARGET_VIBRANT_SATURATION, MIN_VIBRANT_SATURATION, 1f);

        mMutedSwatch = findColor(TARGET_NORMAL_LUMA, MIN_NORMAL_LUMA, MAX_NORMAL_LUMA,
                TARGET_MUTED_SATURATION, 0f, MAX_MUTED_SATURATION);

        mLightMutedColor = findColor(TARGET_LIGHT_LUMA, MIN_LIGHT_LUMA, 1f,
                TARGET_MUTED_SATURATION, 0f, MAX_MUTED_SATURATION);

        mDarkMutedSwatch = findColor(TARGET_DARK_LUMA, 0f, MAX_DARK_LUMA,
                TARGET_MUTED_SATURATION, 0f, MAX_MUTED_SATURATION);

        // Now try and generate any missing colors
        generateEmptySwatches();
    }

    /**
     * Generate a {@link net.pierrox.lightning_launcher.script.api.palette.Palette} from a {@link android.graphics.Bitmap} using the default number of colors.
     */
    public static Palette generate(Bitmap bitmap) {
        return generate(bitmap, DEFAULT_CALCULATE_NUMBER_COLORS);
    }

    /**
     * Generate a {@link net.pierrox.lightning_launcher.script.api.palette.Palette} from a {@link android.graphics.Bitmap} using the specified {@code numColors}.
     * Good values for {@code numColors} depend on the source image type.
     * For landscapes, a good values are in the range 12-16. For images which are largely made up
     * of people's faces then this value should be increased to 24-32.
     *
     * @param numColors The maximum number of colors in the generated palette. Increasing this
     *                  number will increase the time needed to compute the values.
     */
    public static Palette generate(Bitmap bitmap, int numColors) {
        checkBitmapParam(bitmap);
        checkNumberColorsParam(numColors);

        // First we'll scale down the bitmap so it's shortest dimension is 100px
        final Bitmap scaledBitmap = scaleBitmapDown(bitmap);

        // Now generate a quantizer from the Bitmap
        ColorCutQuantizer quantizer = ColorCutQuantizer.fromBitmap(scaledBitmap, numColors);

        // If created a new bitmap, recycle it
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle();
        }

        // Now return a ColorExtractor instance
        return new Palette(quantizer.getQuantizedColors());
    }

    /**
     * Generate a {@link net.pierrox.lightning_launcher.script.api.palette.Palette} asynchronously. {@link net.pierrox.lightning_launcher.script.api.palette.Palette.PaletteAsyncListener#onGenerated(net.pierrox.lightning_launcher.script.api.palette.Palette)}
     * will be called with the created instance. The resulting {@link net.pierrox.lightning_launcher.script.api.palette.Palette} is the same as
     * what would be created by calling {@link #generate(android.graphics.Bitmap)}.
     *
     * @param listener Listener to be invoked when the {@link net.pierrox.lightning_launcher.script.api.palette.Palette} has been generated.
     * @return the {@link android.os.AsyncTask} used to asynchronously generate the instance.
     */
    public static AsyncTask<Bitmap, Void, Palette> generateAsync(
            Bitmap bitmap, PaletteAsyncListener listener) {
        return generateAsync(bitmap, DEFAULT_CALCULATE_NUMBER_COLORS, listener);
    }

    /**
     * Generate a {@link net.pierrox.lightning_launcher.script.api.palette.Palette} asynchronously. {@link net.pierrox.lightning_launcher.script.api.palette.Palette.PaletteAsyncListener#onGenerated(net.pierrox.lightning_launcher.script.api.palette.Palette)}
     * will be called with the created instance. The resulting {@link net.pierrox.lightning_launcher.script.api.palette.Palette} is the same as what
     * would be created by calling {@link #generate(android.graphics.Bitmap, int)}.
     *
     * @param listener Listener to be invoked when the {@link net.pierrox.lightning_launcher.script.api.palette.Palette} has been generated.
     * @return the {@link android.os.AsyncTask} used to asynchronously generate the instance.
     */
    public static AsyncTask<Bitmap, Void, Palette> generateAsync(
            final Bitmap bitmap, final int numColors, final PaletteAsyncListener listener) {
        checkBitmapParam(bitmap);
        checkNumberColorsParam(numColors);
        checkAsyncListenerParam(listener);

        return new AsyncTask<Bitmap, Void, Palette>() {
            @Override
            protected Palette doInBackground(Bitmap... params) {
                return generate(bitmap, numColors);
            }

            @Override
            protected void onPostExecute(Palette colorExtractor) {
                try {
                    listener.onGenerated(colorExtractor);
                } catch (RhinoException e) {
                    ScriptExecutor.getCurrent().displayScriptError(e);
                }
            }
        };
    }

    /**
     * Scale the bitmap down so that it's smallest dimension is
     * {@value #CALCULATE_BITMAP_MIN_DIMENSION}px. If {@code bitmap} is smaller than this, than it
     * is returned.
     */
    private static Bitmap scaleBitmapDown(Bitmap bitmap) {
        final int minDimension = Math.min(bitmap.getWidth(), bitmap.getHeight());

        if (minDimension <= CALCULATE_BITMAP_MIN_DIMENSION) {
            // If the bitmap is small enough already, just return it
            return bitmap;
        }

        final float scaleRatio = CALCULATE_BITMAP_MIN_DIMENSION / (float) minDimension;
        return Bitmap.createScaledBitmap(bitmap,
                Math.round(bitmap.getWidth() * scaleRatio),
                Math.round(bitmap.getHeight() * scaleRatio),
                false);
    }

    private static float createComparisonValue(float saturation, float targetSaturation,
                                               float luma, float targetLuma,
                                               int population, int highestPopulation) {
        return weightedMean(
                invertDiff(saturation, targetSaturation), 3f,
                invertDiff(luma, targetLuma), 6.5f,
                population / (float) highestPopulation, 0.5f
        );
    }

    /**
     * Copy a {@link net.pierrox.lightning_launcher.script.api.palette.Palette.Swatch}'s HSL values into a new float[].
     */
    private static float[] copyHslValues(Swatch color) {
        final float[] newHsl = new float[3];
        System.arraycopy(color.getHsl(), 0, newHsl, 0, 3);
        return newHsl;
    }

    /**
     * Returns a value in the range 0-1. 1 is returned when {@code value} equals the
     * {@code targetValue} and then decreases as the absolute difference between {@code value} and
     * {@code targetValue} increases.
     *
     * @param value       the item's value
     * @param targetValue the value which we desire
     */
    private static float invertDiff(float value, float targetValue) {
        return 1f - Math.abs(value - targetValue);
    }

    private static float weightedMean(float... values) {
        float sum = 0f;
        float sumWeight = 0f;

        for (int i = 0; i < values.length; i += 2) {
            float value = values[i];
            float weight = values[i + 1];

            sum += (value * weight);
            sumWeight += weight;
        }

        return sum / sumWeight;
    }

    private static void checkBitmapParam(Bitmap bitmap) {
        if (bitmap == null) {
            throw new IllegalArgumentException("bitmap can not be null");
        }
        if (bitmap.isRecycled()) {
            throw new IllegalArgumentException("bitmap can not be recycled");
        }
    }

    private static void checkNumberColorsParam(int numColors) {
        if (numColors < 1) {
            throw new IllegalArgumentException("numColors must be 1 of greater");
        }
    }

    private static void checkAsyncListenerParam(PaletteAsyncListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener can not be null");
        }
    }

    /**
     * Returns all of the swatches which make up the palette.
     */
    public List<Swatch> getSwatches() {
        return Collections.unmodifiableList(mSwatches);
    }

    /**
     * Returns the most vibrant swatch in the palette. Might be null.
     */
    public Swatch getVibrantSwatch() {
        return mVibrantSwatch;
    }

    /**
     * Returns a light and vibrant swatch from the palette. Might be null.
     */
    public Swatch getLightVibrantSwatch() {
        return mLightVibrantSwatch;
    }

    /**
     * Returns a dark and vibrant swatch from the palette. Might be null.
     */
    public Swatch getDarkVibrantSwatch() {
        return mDarkVibrantSwatch;
    }

    /**
     * Returns a muted swatch from the palette. Might be null.
     */
    public Swatch getMutedSwatch() {
        return mMutedSwatch;
    }

    /**
     * Returns a muted and light swatch from the palette. Might be null.
     */
    public Swatch getLightMutedSwatch() {
        return mLightMutedColor;
    }

    /**
     * Returns a muted and dark swatch from the palette. Might be null.
     */
    public Swatch getDarkMutedSwatch() {
        return mDarkMutedSwatch;
    }

    /**
     * Returns the most vibrant color in the palette as an RGB packed int.
     *
     * @param defaultColor value to return if the swatch isn't available
     */
    public int getVibrantColor(int defaultColor) {
        return mVibrantSwatch != null ? mVibrantSwatch.getRgb() : defaultColor;
    }

    /**
     * Returns a light and vibrant color from the palette as an RGB packed int.
     *
     * @param defaultColor value to return if the swatch isn't available
     */
    public int getLightVibrantColor(int defaultColor) {
        return mLightVibrantSwatch != null ? mLightVibrantSwatch.getRgb() : defaultColor;
    }

    /**
     * Returns a dark and vibrant color from the palette as an RGB packed int.
     *
     * @param defaultColor value to return if the swatch isn't available
     */
    public int getDarkVibrantColor(int defaultColor) {
        return mDarkVibrantSwatch != null ? mDarkVibrantSwatch.getRgb() : defaultColor;
    }

    /**
     * Returns a muted color from the palette as an RGB packed int.
     *
     * @param defaultColor value to return if the swatch isn't available
     */
    public int getMutedColor(int defaultColor) {
        return mMutedSwatch != null ? mMutedSwatch.getRgb() : defaultColor;
    }

    /**
     * Returns a muted and light color from the palette as an RGB packed int.
     *
     * @param defaultColor value to return if the swatch isn't available
     */
    public int getLightMutedColor(int defaultColor) {
        return mLightMutedColor != null ? mLightMutedColor.getRgb() : defaultColor;
    }

    /**
     * Returns a muted and dark color from the palette as an RGB packed int.
     *
     * @param defaultColor value to return if the swatch isn't available
     */
    public int getDarkMutedColor(int defaultColor) {
        return mDarkMutedSwatch != null ? mDarkMutedSwatch.getRgb() : defaultColor;
    }

    /**
     * @return true if we have already selected {@code swatch}
     */
    private boolean isAlreadySelected(Swatch swatch) {
        return mVibrantSwatch == swatch || mDarkVibrantSwatch == swatch ||
                mLightVibrantSwatch == swatch || mMutedSwatch == swatch ||
                mDarkMutedSwatch == swatch || mLightMutedColor == swatch;
    }

    private Swatch findColor(float targetLuma, float minLuma, float maxLuma,
                             float targetSaturation, float minSaturation, float maxSaturation) {
        Swatch max = null;
        float maxValue = 0f;

        for (Swatch swatch : mSwatches) {
            final float sat = swatch.getHsl()[1];
            final float luma = swatch.getHsl()[2];

            if (sat >= minSaturation && sat <= maxSaturation &&
                    luma >= minLuma && luma <= maxLuma &&
                    !isAlreadySelected(swatch)) {
                float thisValue = createComparisonValue(sat, targetSaturation, luma, targetLuma,
                        swatch.getPopulation(), mHighestPopulation);
                if (max == null || thisValue > maxValue) {
                    max = swatch;
                    maxValue = thisValue;
                }
            }
        }

        return max;
    }

    /**
     * Try and generate any missing swatches from the swatches we did find.
     */
    private void generateEmptySwatches() {
        if (mVibrantSwatch == null) {
            // If we do not have a vibrant color...
            if (mDarkVibrantSwatch != null) {
                // ...but we do have a dark vibrant, generate the value by modifying the luma
                final float[] newHsl = copyHslValues(mDarkVibrantSwatch);
                newHsl[2] = TARGET_NORMAL_LUMA;
                mVibrantSwatch = new Swatch(ColorUtils.HSLtoRGB(newHsl), 0);
            }
        }

        if (mDarkVibrantSwatch == null) {
            // If we do not have a dark vibrant color...
            if (mVibrantSwatch != null) {
                // ...but we do have a vibrant, generate the value by modifying the luma
                final float[] newHsl = copyHslValues(mVibrantSwatch);
                newHsl[2] = TARGET_DARK_LUMA;
                mDarkVibrantSwatch = new Swatch(ColorUtils.HSLtoRGB(newHsl), 0);
            }
        }
    }

    /**
     * Find the {@link net.pierrox.lightning_launcher.script.api.palette.Palette.Swatch} with the highest population value and return the population.
     */
    private int findMaxPopulation() {
        int population = 0;
        for (Swatch swatch : mSwatches) {
            population = Math.max(population, swatch.getPopulation());
        }
        return population;
    }

    /**
     * Listener to be used with {@link #generateAsync(android.graphics.Bitmap, net.pierrox.lightning_launcher.script.api.palette.Palette.PaletteAsyncListener)} or
     * {@link #generateAsync(android.graphics.Bitmap, int, net.pierrox.lightning_launcher.script.api.palette.Palette.PaletteAsyncListener)}
     */
    public interface PaletteAsyncListener {

        /**
         * Called when the {@link net.pierrox.lightning_launcher.script.api.palette.Palette} has been generated.
         */
        void onGenerated(Palette palette);
    }

    /**
     * Represents a color swatch generated from an image's palette. The RGB color can be retrieved
     * by calling {@link #getRgb()}.
     */
    public static final class Swatch {

        final int mRed, mGreen, mBlue;
        final int mRgb;
        final int mPopulation;

        private float[] mHsl;

        Swatch(int rgbColor, int population) {
            mRed = Color.red(rgbColor);
            mGreen = Color.green(rgbColor);
            mBlue = Color.blue(rgbColor);
            mRgb = rgbColor;
            mPopulation = population;
        }

        Swatch(int red, int green, int blue, int population) {
            mRed = red;
            mGreen = green;
            mBlue = blue;
            mRgb = Color.rgb(red, green, blue);
            mPopulation = population;
        }

        /**
         * @return this swatch's RGB color value
         */
        public int getRgb() {
            return mRgb;
        }

        /**
         * Return this swatch's HSL values.
         * hsv[0] is Hue [0 .. 360)
         * hsv[1] is Saturation [0...1]
         * hsv[2] is Lightness [0...1]
         */
        public float[] getHsl() {
            if (mHsl == null) {
                // Lazily generate HSL values from RGB
                mHsl = new float[3];
                ColorUtils.RGBtoHSL(mRed, mGreen, mBlue, mHsl);
            }
            return mHsl;
        }

        /**
         * @return the number of pixels represented by this swatch
         */
        public int getPopulation() {
            return mPopulation;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " " +
                    "[" + Integer.toHexString(getRgb()) + ']' +
                    "[HSL: " + Arrays.toString(getHsl()) + ']' +
                    "[Population: " + mPopulation + ']';
        }
    }

}
