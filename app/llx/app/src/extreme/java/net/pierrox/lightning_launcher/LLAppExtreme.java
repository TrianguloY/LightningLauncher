/*
MIT License

Copyright (c) 2022 Pierre Hébert

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package net.pierrox.lightning_launcher;

import android.content.Context;
import android.view.View;

import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.prefs.LLPreference;

public class LLAppExtreme extends LLAppPhone {

    @Override
    public boolean isFreeVersion() {
        return false;
    }

    @Override
    public boolean isTrialVersion() {
        return false;
    }

    @Override
    public boolean isTrialVersionExpired() {
        return false;
    }

    @Override
    public long getTrialLeft() {
        return 0;
    }

    @Override
    public View managePreferenceViewLockedFlag(LLPreference preference, View preference_view) {
        return preference_view;
    }

    @Override
    public void manageAddItemDialogLockedFlag(View add_item_view, boolean locked) {
        // pass
    }

    @Override
    public void showFeatureLockedDialog(Context context) {
        // pass
    }

    @Override
    public void startUnlockProcess(Context context) {
        // pass
    }

    @Override
    public void installPromotionalIcons(Page dashboard) {
        // pass
    }

    @Override
    public void checkLicense() {
        // pass
    }

    public void checkLwpKey() {
        // pass
    }

    public boolean hasLWP() {
        return true;
    }

    public void showProductLockedDialog(final Context context, int title, int message, final String sku) {
        // pass
    }
}
