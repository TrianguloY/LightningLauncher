/*
MIT License

Copyright (c) 2022 Pierre Hébert, TrianguloY

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

package net.pierrox.lightning_launcher.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.Version;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.script.Script;
import net.pierrox.lightning_launcher.script.ScriptManager;
import net.pierrox.lightning_launcher.script.api.Box;
import net.pierrox.lightning_launcher.script.api.Container;
import net.pierrox.lightning_launcher.script.api.Desktop;
import net.pierrox.lightning_launcher.script.api.Event;
import net.pierrox.lightning_launcher.script.api.Folder;
import net.pierrox.lightning_launcher.script.api.Image;
import net.pierrox.lightning_launcher.script.api.ImageAnimation;
import net.pierrox.lightning_launcher.script.api.ImageBitmap;
import net.pierrox.lightning_launcher.script.api.ImageNinePatch;
import net.pierrox.lightning_launcher.script.api.ImageScript;
import net.pierrox.lightning_launcher.script.api.ImageSvg;
import net.pierrox.lightning_launcher.script.api.Item;
import net.pierrox.lightning_launcher.script.api.Lightning;
import net.pierrox.lightning_launcher.script.api.PageIndicator;
import net.pierrox.lightning_launcher.script.api.Panel;
import net.pierrox.lightning_launcher.script.api.PropertyEditor;
import net.pierrox.lightning_launcher.script.api.PropertySet;
import net.pierrox.lightning_launcher.script.api.RectL;
import net.pierrox.lightning_launcher.script.api.StopPoint;
import net.pierrox.lightning_launcher.util.FileAndDirectoryPickerDialog;
import net.pierrox.lightning_launcher.util.FileProvider;
import net.pierrox.lightning_launcher.util.Indentation;
import net.pierrox.lightning_launcher.util.Search;
import net.pierrox.lightning_launcher_extreme.BuildConfig;
import net.pierrox.lightning_launcher_extreme.R;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import fr.xgouchet.texteditor.ui.AdvancedEditText;

public class ScriptEditor extends ResourceWrapperActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {
    public static final String SIS_SEL_START = "s";
    public static final String SIS_SEL_END = "e";
    public static final String SIS_SCROLL_X = "x";
    public static final String SIS_SCROLL_Y = "y";
    private static final String INTENT_EXTRA_SCRIPT_ID = "i";
    private static final String INTENT_EXTRA_SCRIPT_LINE = "l";
    private static final int REQUEST_EDIT_SCRIPT = 0;

    private static final String PREF_LAST_SCRIPT_ID = "se_lsi";
    private static final String PREF_LAST_SCRIPT_LINE = "se_lsl";
    private static final String PREF_WORDWRAP = "se_w";
    private static final String PREF_AUTOINDENT = "se_ind";
    private static final String PREF_FONT_SIZE = "se_fs";
    private static final String PREF_DIRECTORY = "se_d";
    private static final String PREF_SUB_DIRS = "se_sd";

    private static final int DIALOG_EDIT_SCRIPT = 0;
    private static final int DIALOG_CONFIRM_DELETE = 1;
    private static final int DIALOG_CHECK_RESULT = 2;
    private static final int DIALOG_SCRIPT_IMPORTER_NOT_INSTALLED = 3;
    private static final SparseArray<TextPosition> sScriptPositions = new SparseArray<>();
    private static final Token[] sMainTokens = new Token[]{
            new TokenMethod(Lightning.class, "getEvent", "void", new Class[0])
    };
    private static ArrayList<TokenMethod> sAutoCompleteTokens;
    private final List<Script> mAllScripts = new ArrayList<>();
    private final View.OnLongClickListener mCompletionButtonLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            final ArrayList<Token> tokens = (ArrayList<Token>) v.getTag();
            int size = tokens.size();
            if (size == 1) {
                displayTokenApiReference(tokens.get(0));
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(ScriptEditor.this);
                String[] items = new String[size];
                for (int i = 0; i < size; i++) {
                    items[i] = tokens.get(i).getUnambiguousDisplayName();
                }
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        displayTokenApiReference(tokens.get(which));
                    }
                });
                builder.create().show();
            }
            return true;
        }
    };
    // list of shortcuts to display
    private final Shortcut[] mShortcuts = new Shortcut[]{
            new ShortcutKey(KeyEvent.KEYCODE_DPAD_LEFT, "|"),
            new ShortcutKey(KeyEvent.KEYCODE_DPAD_UP, "~"),
            new ShortcutKey(KeyEvent.KEYCODE_DPAD_DOWN, "{"),
            new ShortcutKey(KeyEvent.KEYCODE_DPAD_RIGHT, "}"),
            new ShortcutAction(SA.DEC_TAB),
            new ShortcutAction(SA.INC_TAB),
            new ShortcutText("(", ")"),
            new ShortcutText("[", "]"),
            new ShortcutText("{", "}"),
            new ShortcutText("var ", ""),
            new ShortcutAction(SA.SEARCH),
            new ShortcutAction(SA.SEARCHN),
    };
    private InputMethodManager mInputMethodManager;
    private ScriptManager mScriptManager;
    private Script mScript;
    private File mCurrentDirectory;
    private View mLeftPane;
    private ViewGroup mCompletionsViewGroup;
    private Spinner mScriptSpinner;
    private AdvancedEditText mScriptText;
    private final View.OnClickListener mShortcutButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button btn = (Button) v;
            Shortcut shortcut = (Shortcut) btn.getTag();
            shortcut.apply(mScriptText);
            //mInputMethodManager.restartInput(mScriptText);
        }
    };
    private CheckBox mMenuLightning;
    private CheckBox mMenuItem;
    private CheckBox mMenuCustom;
    private ArrayAdapter<Script> mScriptAdapter;
    private Indentation mIndentation;
    private Search mSearch;
    private boolean mShowSubDirs;
    private int mCompletionStart;
    private int mCompletionEnd;
    private final View.OnClickListener mCompletionButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button btn = (Button) v;
            ArrayList<Token> tokens = (ArrayList<Token>) btn.getTag();
            Object firstToken = tokens.get(0);
            String text;
            int selection_offset;
            if (firstToken.getClass() == TokenClass.class) {
                TokenClass token = (TokenClass) firstToken;
                text = token.getDisplayName();
                selection_offset = text.length();
            } else {
                TokenMethod token = (TokenMethod) firstToken;
                text = token.getDisplayName() + "(";
                selection_offset = text.length() + (token.args.length > 0 ? 0 : 1);
//				for(int i = 0; i< length; i++) {
//					if(i > 0) {
//						text += ",";
//					}
//				}
                text += ")";
            }
            int start, end;
            final int selectionStart = mScriptText.getSelectionStart();
            if ("LL".equals(text)) {
                start = selectionStart;
                end = selectionStart;
            } else {
                start = mCompletionStart;
                end = mCompletionEnd;
            }
            mScriptText.getEditableText().replace(start, end, text);
            mScriptText.setSelection(start + selection_offset);
            mCompletionsViewGroup.removeAllViews();
            mInputMethodManager.restartInput(mScriptText);
        }
    };
    TextWatcher mScriptTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//                Log.i("XXX", "before "+s + " " + start + " " + after + " " + count);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // isolate token
            int seqStart = findTokenStart(s, start);
            int seqEnd = findTokenEnd(s, start);

            // trigger autocompletion starting at 3 characters
            if ((seqEnd - seqStart) >= 1) {
                // try to find the previous token
                String previousToken = null;
                int n = seqStart - 1;
                while (n >= 0 && !Character.isLetterOrDigit(s.charAt(n))) {
                    n--;
                }
                if (n > 0) {
                    int m = n;
                    while (m >= 0 && Character.isLetterOrDigit(s.charAt(m))) {
                        m--;
                    }
                    previousToken = s.subSequence(m + 1, n + 1).toString();
                }


                String token = s.subSequence(seqStart, seqEnd).toString().toLowerCase();

                mCompletionStart = seqStart;
                mCompletionEnd = seqEnd;

                updateSuggestionsList(token, previousToken);
            } else {
                mCompletionStart = start;
                mCompletionEnd = start;
                updateSuggestionsList("-", null);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };
    private Button mSelectDirectoryButton;
    private float mScaledDensity;
    private Animation mLeftPaneAnimIn;
    private Animation mLeftPaneAnimOut;
    private SharedPreferences mSharedPrefs;

    public static void startActivity(Context context, int script_id, int line) {
        Intent intent = new Intent(context, ScriptEditor.class);
        intent.putExtra(INTENT_EXTRA_SCRIPT_ID, script_id);
        intent.putExtra(INTENT_EXTRA_SCRIPT_LINE, line);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mInputMethodManager = ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE));

        LightningEngine engine = LLApp.get().getAppEngine();
        mScriptManager = engine.getScriptManager();

        String p = mSharedPrefs.getString(PREF_DIRECTORY, null);
        if (p == null) {
            mCurrentDirectory = mScriptManager.getScriptsDir();
        } else {
            mCurrentDirectory = new File(p);
        }

        mScaledDensity = getResources().getDisplayMetrics().scaledDensity;

        setContentView(R.layout.script_editor);

        mLeftPane = findViewById(R.id.left_pane);

        final GestureDetector gd = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (e2.getX() - e1.getX() < 0) {
                    hideLeftPane();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                hideLeftPane();
                return true;
            }
        });
        findViewById(R.id.left_pane_c).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gd.onTouchEvent(event);
                return true;
            }
        });

        mLeftPaneAnimIn = AnimationUtils.makeInAnimation(this, true);
        mLeftPaneAnimOut = AnimationUtils.makeOutAnimation(this, false);

//        mCompletionsListView = (ListView) findViewById(R.id.completions);
        mCompletionsViewGroup = findViewById(R.id.completions);
        initializeShortcuts(findViewById(R.id.shortcuts));

        Button btn;

        btn = findViewById(R.id.sc_import);
        btn.setOnClickListener(this);
        btn.setText(R.string.sc_import);
        btn = findViewById(R.id.sc_new);
        btn.setOnClickListener(this);
        btn.setText(R.string.sc_new);
        btn = findViewById(R.id.sc_delete);
        btn.setOnClickListener(this);
        btn.setText(R.string.sc_delete);
        btn = findViewById(R.id.sc_edit);
        btn.setOnClickListener(this);
        btn.setText(R.string.sc_edit);
        btn = findViewById(R.id.sc_help);
        btn.setOnClickListener(this);
        btn.setText(R.string.sc_help);
        btn = findViewById(R.id.sc_send);
        btn.setOnClickListener(this);
        btn.setText(R.string.sc_send);

        mScriptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mAllScripts);
        mScriptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ((TextView) findViewById(R.id.sc_path)).setText(R.string.sc_path);
        ((TextView) findViewById(R.id.sc_name)).setText(R.string.sc_name);
        mScriptSpinner = findViewById(R.id.sc_spinner);
        mScriptSpinner.setLongClickable(true);
        mScriptSpinner.setOnItemSelectedListener(this);
        mScriptSpinner.setAdapter(mScriptAdapter);
        updateScriptsSpinner();

        btn = findViewById(R.id.sc_edit_name);
        btn.setOnClickListener(this);
        btn.setTypeface(LLApp.get().getIconsTypeface());

        if (sAutoCompleteTokens == null) {
            buildAutoCompleteTokens();
        }
        mScriptText = findViewById(R.id.sc_text);
        mScriptText.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        mScriptText.addTextChangedListener(mScriptTextWatcher);
        mScriptText.setListener(new AdvancedEditText.OnAdvancedEditTextEvent() {
            private float mInitialTextSize;

            @Override
            public boolean onLeftEdgeSwipe() {
                showLeftPane();
                return true;
            }

            @Override
            public boolean onTap() {
                if (mLeftPane.getVisibility() == View.VISIBLE) {
                    hideLeftPane();
                    return true;
                } else {
                    mCompletionsViewGroup.removeAllViews();
                    return false;
                }
            }

            @Override
            public void onPinchStart() {
                mInitialTextSize = mScriptText.getTextSize();
            }

            @Override
            public void onPinchZoom(double scale) {
                float size = (float) (scale * mInitialTextSize / mScaledDensity);
                if (size < 3) size = 3;
                else if (size > 150) size = 150;
                mScriptText.setTextSize(size);
            }
        });
        mIndentation = new Indentation();
        mSearch = new Search(this, mScriptText);

        ((TextView) findViewById(R.id.sc_ma)).setText(R.string.sc_ma);
        ((TextView) findViewById(R.id.sc_a)).setText(R.string.sc_a);
        ((TextView) findViewById(R.id.sc_h)).setText(R.string.sc_h);
        mMenuLightning = findViewById(R.id.sc_ml);
        mMenuLightning.setText(R.string.sc_ml);
        mMenuItem = findViewById(R.id.sc_mi);
        mMenuItem.setText(R.string.sc_mi);
        mMenuCustom = findViewById(R.id.sc_mc);
        mMenuCustom.setText(R.string.sc_mc);

        mShowSubDirs = mSharedPrefs.getBoolean(PREF_SUB_DIRS, true);
        CheckBox showSubDirs = findViewById(R.id.sc_sd);
        showSubDirs.setText(R.string.sc_all);
        showSubDirs.setChecked(mShowSubDirs);
        showSubDirs.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mShowSubDirs = isChecked;
                updateScriptsSpinner();
                if (!mAllScripts.contains(mScript) && mAllScripts.size() > 0) {
                    Script script = mAllScripts.get(0);
                    displayScript(script);
                }
            }
        });

        mSelectDirectoryButton = findViewById(R.id.sc_d);
        mSelectDirectoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileAndDirectoryPickerDialog.showForScriptDirectory(ScriptEditor.this, mCurrentDirectory, mScriptManager, new FileAndDirectoryPickerDialog.FileAndDirectoryPickerDialogListener() {
                    @Override
                    public void onFileSelected(File file, File newDirectory) {
                        mCurrentDirectory = file;
                        updateSelectDirectoryButton();
                        updateScriptsSpinner();
                        if (!mAllScripts.contains(mScript) && mAllScripts.size() > 0) {
                            Script script = mAllScripts.get(0);
                            displayScript(script);
                        }
                    }
                });
            }
        });
        updateSelectDirectoryButton();


        boolean wordwrap = mSharedPrefs.getBoolean(PREF_WORDWRAP, true);
        mScriptText.setWordWrap(wordwrap);
        boolean autoindent = mSharedPrefs.getBoolean(PREF_AUTOINDENT, true);
        if (autoindent) mIndentation.register(mScriptText);
        float text_size = mSharedPrefs.getFloat(PREF_FONT_SIZE, mScriptText.getTextSize() / mScaledDensity);
        mScriptText.setTextSize(text_size);
        ((TextView) findViewById(R.id.sc_o)).setText(R.string.sc_o);
        CheckBox wordwrap_checkbox = findViewById(R.id.sc_w);
        wordwrap_checkbox.setText(R.string.sc_w);
        wordwrap_checkbox.setChecked(wordwrap);
        wordwrap_checkbox.setOnCheckedChangeListener(this);
        CheckBox autoindent_checkbox = findViewById(R.id.sc_ind);
        autoindent_checkbox.setText(R.string.sc_ind);
        autoindent_checkbox.setChecked(autoindent);
        autoindent_checkbox.setOnCheckedChangeListener(this);


        TextPosition position = null;
        int sel_start = 0, sel_end = 0;
        Intent intent = getIntent();
        int goToLine = -1;
        if (intent.hasExtra(INTENT_EXTRA_SCRIPT_ID)) {
            int id = getIntent().getIntExtra(INTENT_EXTRA_SCRIPT_ID, Script.NO_ID);
            if (id != Script.NO_ID) {
                mScript = mScriptManager.getOrLoadScript(id);
                goToLine = intent.getIntExtra(INTENT_EXTRA_SCRIPT_LINE, -1);
            }
            if (mScript == null && id != Script.NO_ID) {
                Toast.makeText(this, R.string.sc_deleted, Toast.LENGTH_SHORT).show();
                goToLine = 1;
            }
        } else if (savedInstanceState != null) {
            mScript = mScriptManager.getOrLoadScript(savedInstanceState.getInt(INTENT_EXTRA_SCRIPT_ID));
            if (mScript != null) {
                sel_start = savedInstanceState.getInt(SIS_SEL_START);
                sel_end = savedInstanceState.getInt(SIS_SEL_END);
                int x = savedInstanceState.getInt(SIS_SCROLL_X, 0);
                int y = savedInstanceState.getInt(SIS_SCROLL_Y, 0);
                mScriptText.scrollTo(x, y);
            }
        }

        int last_script_id = mSharedPrefs.getInt(PREF_LAST_SCRIPT_ID, Script.NO_ID);
        if (mScript == null && last_script_id != Script.NO_ID) {
            mScript = mScriptManager.getOrLoadScript(last_script_id);

            if (mScript == null) {
                // script deleted, go up as long as the directory is not valid
                while (!mCurrentDirectory.exists()) {
                    mCurrentDirectory = mCurrentDirectory.getParentFile();
                }
                updateScriptsSpinner();
            } else {
                // use the in-memory exact saved position, otherwise use the line number
                position = sScriptPositions.get(mScript.id);
                if (position == null) {
                    goToLine = mSharedPrefs.getInt(PREF_LAST_SCRIPT_LINE, -1);
                }
            }
        }

        if (mScript == null && mAllScripts.size() > 0) {
            mScript = mAllScripts.get(0);
        }

        if (mScript == null) {
            createNewScript();
            goToLine = 1;
        } else {
            if (mScript.getType() == Script.TYPE_FILE) {
                if (!mShowSubDirs) {
                    mCurrentDirectory = mScript.getFile().getParentFile();
                }
                updateScriptsSpinner();
                updateSelectDirectoryButton();
            }
        }

        int index = mAllScripts.indexOf(mScript);
        if (index == -1) {
            mCurrentDirectory = mScriptManager.getScriptsDir();
            updateScriptsSpinner();
            updateSelectDirectoryButton();
            mAllScripts.add(mScript);
            mScriptAdapter.notifyDataSetChanged();
            index = mAllScripts.indexOf(mScript);
        }
        mScriptSpinner.setSelection(index);
        updateViews();
        if (position == null) {
            mScriptText.setSelection(sel_start, sel_end);
        } else {
            position.apply(mScriptText);
        }
        if (goToLine != -1) {
            mScriptText.scrollToLine(goToLine);
        }

        if (!engine.getGlobalConfig().runScripts) {
            Toast.makeText(this, R.string.rs_w, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateScriptsSpinner() {
        ArrayList<Script> allScripts = mScriptManager.getAllScriptMatching(Script.FLAG_ALL);
        String currentPath = mScriptManager.getRelativePath(mCurrentDirectory);
        if (!currentPath.endsWith("/")) {
            // ensure a trailing slash to avoid false match (like /ab and /ac whith current = /a)
            currentPath += "/";
        }
        mAllScripts.clear();
        for (Script script : allScripts) {
            if (script.getType() == Script.TYPE_FILE) {
                String relativePath = script.getRelativePath();
                if (!relativePath.endsWith("/")) {
                    relativePath += "/";
                }
                if ((mShowSubDirs && relativePath.startsWith(currentPath)) || relativePath.equals(currentPath)) {
                    mAllScripts.add(script);
                }
            }
        }
        Utils.sortScripts(mAllScripts);

        mScriptAdapter.notifyDataSetChanged();
        int position = mAllScripts.indexOf(mScript);
        if (position != -1) {
            mScriptSpinner.setSelection(position);
        }
    }

    private void updateSelectDirectoryButton() {
        mSelectDirectoryButton.setText(mScriptManager.getRelativePath(mCurrentDirectory));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(INTENT_EXTRA_SCRIPT_ID, mScript.id);
        outState.putInt(SIS_SEL_START, mScriptText.getSelectionStart());
        outState.putInt(SIS_SEL_END, mScriptText.getSelectionEnd());
        outState.putInt(SIS_SCROLL_X, mScriptText.getScrollX());
        outState.putInt(SIS_SCROLL_Y, mScriptText.getScrollY());
    }

    @Override
    protected void onPause() {
        super.onPause();

        saveScript();

        mSharedPrefs.edit()
                .putFloat(PREF_FONT_SIZE, mScriptText.getTextSize() / mScaledDensity)
                .putString(PREF_DIRECTORY, mCurrentDirectory.getAbsolutePath())
                .putBoolean(PREF_SUB_DIRS, mShowSubDirs)
                .apply();

        if (mScript.id >= 0) {
            setLastScriptId();
        }
    }

    @Override
    public void onBackPressed() {
        if (mLeftPane.getVisibility() == View.VISIBLE) {
            hideLeftPane();
        } else {
            Intent data = new Intent();
            data.putExtra(INTENT_EXTRA_SCRIPT_ID, mScript.id);
            setResult(RESULT_OK, data);

            finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_TAB) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_TAB) {
            if (event.isShiftPressed()) {
                // Shift tab, decrease indent
                Pair<Integer, Integer> selectionI =
                        Indentation.modifyIndent(mScriptText.getTrueSelectionStart(), mScriptText.getTrueSelectionEnd(), false, mScriptText.getEditableText());
                mScriptText.setSelection(selectionI.first, selectionI.second);
            } else if (mScriptText.hasSelection()) {
                // No shift tab && selection, increase indent
                Pair<Integer, Integer> selectionI =
                        Indentation.modifyIndent(mScriptText.getTrueSelectionStart(), mScriptText.getTrueSelectionEnd(), true, mScriptText.getEditableText());
                mScriptText.setSelection(selectionI.first, selectionI.second);
            } else {
                // No shift tab && no selection, add tab char
                mScriptText.getText().insert(mScriptText.getSelectionStart(), "\t");
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder;

        switch (id) {
            case DIALOG_EDIT_SCRIPT:
                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.sc_dn_t);

                View content = getLayoutInflater().inflate(R.layout.edit_script_dialog, null);

                final EditText nameEditText = content.findViewById(R.id.sc_name);
                nameEditText.setText(mScript.name);
                nameEditText.setSelection(mScript.name.length());

                final EditText pathEditText = content.findViewById(R.id.sc_path);
                final Button pickPathButton = content.findViewById(R.id.sc_pick_path);
                pickPathButton.setTypeface(LLApp.get().getIconsTypeface());
                pickPathButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        FileAndDirectoryPickerDialog.showForScriptDirectory(ScriptEditor.this, mCurrentDirectory, mScriptManager, new FileAndDirectoryPickerDialog.FileAndDirectoryPickerDialogListener() {
                            @Override
                            public void onFileSelected(File file, File newDirectory) {
                                pathEditText.setText(mScriptManager.getRelativePath(file));
                            }
                        });
                    }
                });
                final String relativePath;
                if (mScript.getType() == Script.TYPE_FILE) {
                    relativePath = mScriptManager.getRelativePath(mScript.getFile().getParentFile());
                    pathEditText.setText(relativePath);
                    pathEditText.setSelection(relativePath.length());
                } else {
                    relativePath = null;
                    pathEditText.setVisibility(View.GONE);
                    pickPathButton.setVisibility(View.GONE);
                }

                builder.setView(content);
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mScript.name = nameEditText.getText().toString();
                                if (relativePath != null) {
                                    String toRelativePath = ScriptManager.sanitizeRelativePath(pathEditText.getText().toString());
                                    if (!toRelativePath.equals(relativePath)) {
                                        mScript.setRelativePath(toRelativePath);

                                        String sp = mScript.getRelativePath();
                                        if (!sp.endsWith("/")) sp += "/";
                                        String cp = mScriptManager.getRelativePath(mCurrentDirectory);
                                        if (!cp.endsWith("/")) cp += "/";
                                        if (!mShowSubDirs || !sp.startsWith(cp)) {
                                            mCurrentDirectory = new File(mScriptManager.getScriptsDir() + toRelativePath);
                                        }
                                        updateScriptsSpinner();
                                        updateSelectDirectoryButton();
                                    }
                                }
                                mScriptManager.saveScript(mScript);
                                updateScriptsSpinner();
                            }
                        });
                builder.setNegativeButton(android.R.string.cancel, null);
                return builder.create();

            case DIALOG_CONFIRM_DELETE:
                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.sc_dd_t);
                builder.setMessage(R.string.sc_dd_m);
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mScriptManager.deleteScript(mScript);
                                sScriptPositions.delete(mScript.id);
                                int n = mAllScripts.indexOf(mScript);
                                mAllScripts.remove(n);
                                mScript = null;

                                File scriptsDir = mScriptManager.getScriptsDir();
                                File directory = mCurrentDirectory;
                                while (!directory.equals(scriptsDir)) {
                                    String[] list = directory.list();
                                    if (list != null && list.length > 0) {
                                        break;
                                    }
                                    File parent = directory.getParentFile();
                                    directory.delete();
                                    directory = parent;
                                }
                                if (!directory.equals(mCurrentDirectory)) {
                                    mCurrentDirectory = directory;
                                    updateSelectDirectoryButton();
                                    updateScriptsSpinner();
                                    n = 0;
                                }

                                if (mAllScripts.size() == 0) {
                                    createNewScript();
                                } else {
                                    mScript = mAllScripts.get(n > 0 ? n - 1 : n);
                                    updateViews();
                                }
                                mScriptAdapter.notifyDataSetChanged();
                                mScriptSpinner.setSelection(mAllScripts.indexOf(mScript));
                            }
                        });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setCancelable(false);
                return builder.create();

            case DIALOG_CHECK_RESULT:
                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.sc_dc_t);
                builder.setMessage(R.string.sc_dc_ok);
                builder.setPositiveButton(android.R.string.ok, null);
                return builder.create();

            case DIALOG_SCRIPT_IMPORTER_NOT_INSTALLED:
                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.sc_si_t);
                builder.setMessage(R.string.sc_si_m);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse(Version.APP_STORE_INSTALL_PREFIX + Version.SCRIPT_IMPORTER_PKG)), null));
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                return builder.create();
        }
        return super.onCreateDialog(id);
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.sc_import:
                boolean launched = false;
                intent = getPackageManager().getLaunchIntentForPackage(Version.SCRIPT_IMPORTER_PKG);
                if (intent != null) {
                    try {
                        startActivity(intent);
                        launched = true;
                    } catch (ActivityNotFoundException e) {
                        // pass
                    }
                }
                if (!launched) {
                    showDialog(DIALOG_SCRIPT_IMPORTER_NOT_INSTALLED);
                }
                break;

            case R.id.sc_new:
                saveScript();
                createNewScript();
                removeDialog(DIALOG_EDIT_SCRIPT);
                showDialog(DIALOG_EDIT_SCRIPT);
                break;

            case R.id.sc_delete:
                showDialog(DIALOG_CONFIRM_DELETE);
                break;

            case R.id.sc_edit:
                if (!checkPermissions(
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        new int[]{R.string.pr_r8},
                        REQUEST_PERMISSION_BASE)) {
                    break;
                }
                saveScript();

                File from = mScriptManager.getScriptFile(mScript.id);
                File to = new File(FileUtils.LL_TMP_DIR, "script.js");

                // check whether the content is the same to avoid
                // "File changed, reload ?" message in external editors
                String file_text = FileUtils.readFileContent(from);
                String script_text = mScript.getSourceText();
                if (!script_text.equals(file_text)) {
                    try {
                        FileUtils.saveStringToFile(script_text, to);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                String type = "text/javascript";
                intent = new Intent(Intent.ACTION_EDIT);
                Uri uri = FileProvider.getUriForFile(to);
                intent.setDataAndType(uri, type);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                try {
                    startActivityForResult(intent, REQUEST_EDIT_SCRIPT);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "No external editor", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.sc_help:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.pierrox.net/android/applications/lightning_launcher/script/")));
                break;

            case R.id.sc_send:
                saveScript();
                Intent send = new Intent();
                send.setAction(Intent.ACTION_SEND);
                send.putExtra(Intent.EXTRA_TEXT, mScript.getSourceText());
                send.setType("text/plain");
                startActivity(Intent.createChooser(send, null));
                break;

            case R.id.sc_edit_name:
                removeDialog(DIALOG_EDIT_SCRIPT);
                showDialog(DIALOG_EDIT_SCRIPT);
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.sc_w:
                mScriptText.setWordWrap(isChecked);
                mSharedPrefs.edit().putBoolean(PREF_WORDWRAP, isChecked).apply();
                break;
            case R.id.sc_ind:
                // toggled autoindentation
                if (isChecked) {
                    mIndentation.register(mScriptText);
                } else {
                    mIndentation.unregister(mScriptText);
                }
                mSharedPrefs.edit().putBoolean(PREF_AUTOINDENT, isChecked).apply();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EDIT_SCRIPT) {
            File script_file = new File(FileUtils.LL_TMP_DIR, "script.js");
            String new_text = FileUtils.readFileContent(script_file);
            if (!mScript.getSourceText().equals(new_text)) {
                mScriptText.setText(new_text);
            }
            script_file.delete();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void saveScript() {
        mScript.setSourceText(mScriptText.getText().toString());
        mScript.setFlag(Script.FLAG_APP_MENU, mMenuLightning.isChecked());
        mScript.setFlag(Script.FLAG_ITEM_MENU, mMenuItem.isChecked());
        mScript.setFlag(Script.FLAG_CUSTOM_MENU, mMenuCustom.isChecked());
        mScript.setFlag(Script.FLAG_DISABLED, false);
        mScriptManager.saveScript(mScript);

        sScriptPositions.put(mScript.id, new TextPosition(mScriptText));
    }

    private void check() {
        showDialog(DIALOG_CHECK_RESULT);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        Script script = (Script) adapterView.getItemAtPosition(i);
        if (script != mScript) {
            if (mScript != null) {
                saveScript();
            }

            displayScript(script);
        }
    }

    private void displayScript(Script script) {
        mScript = script;
        updateViews();
        TextPosition position = sScriptPositions.get(mScript.id);
        if (position != null) {
            position.apply(mScriptText);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private void createNewScript() {
        mScript = mScriptManager.createScriptForFile(mScriptManager.getDefaultScriptName(this), mScriptManager.getRelativePath(mCurrentDirectory));
        mAllScripts.add(mScript);
        mScriptAdapter.notifyDataSetChanged();
        mScriptSpinner.setSelection(mScriptAdapter.getPosition(mScript));
        updateViews();
    }

    private void setLastScriptId() {
        mSharedPrefs.edit().putInt(PREF_LAST_SCRIPT_ID, mScript.id).apply();
        mSharedPrefs.edit().putInt(PREF_LAST_SCRIPT_LINE, mScriptText.getSelectionLine()).apply();
    }

    private void showLeftPane() {
        if (mLeftPane.getVisibility() == View.GONE) {
            mLeftPane.setVisibility(View.VISIBLE);
            mLeftPane.startAnimation(mLeftPaneAnimIn);
            mInputMethodManager.hideSoftInputFromWindow(mScriptText.getWindowToken(), 0);
        }
    }

    private void hideLeftPane() {
        if (mLeftPane.getVisibility() == View.VISIBLE) {
            mLeftPane.setVisibility(View.GONE);
            mLeftPane.startAnimation(mLeftPaneAnimOut);
            //mInputMethodManager.showSoftInput(mScriptText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void updateViews() {
        mScriptText.setText(mScript.getSourceText());
        mMenuLightning.setChecked(mScript.hasFlag(Script.FLAG_APP_MENU));
        mMenuItem.setChecked(mScript.hasFlag(Script.FLAG_ITEM_MENU));
        mMenuCustom.setChecked(mScript.hasFlag(Script.FLAG_CUSTOM_MENU));

        boolean is_file = mScript.getType() == Script.TYPE_FILE;
        mMenuLightning.setEnabled(is_file);
        mMenuItem.setEnabled(is_file);
        mMenuCustom.setEnabled(is_file);
        findViewById(R.id.sc_delete).setEnabled(is_file);
    }

    private void buildAutoCompleteTokens() {
        sAutoCompleteTokens = new ArrayList<>();
        Class<?>[] classes = {
                Box.class,
                net.pierrox.lightning_launcher.script.api.Binding.class,
                Container.class,
                Desktop.class,
                Event.class,
                Folder.class,
                Image.class,
                ImageBitmap.class,
                ImageNinePatch.class,
                ImageAnimation.class,
                ImageScript.class,
                ImageSvg.class,
                Item.class,
                Lightning.class,
                Panel.class,
                StopPoint.class,
                PageIndicator.class,
                net.pierrox.lightning_launcher.script.api.palette.Palette.class,
                net.pierrox.lightning_launcher.script.api.Property.class,
                PropertyEditor.class,
                PropertySet.class,
                RectL.class,
                net.pierrox.lightning_launcher.script.api.Script.class,
                net.pierrox.lightning_launcher.script.api.Shortcut.class,
                net.pierrox.lightning_launcher.script.api.Lightning.class,
                net.pierrox.lightning_launcher.script.api.Menu.class,
                ComponentName.class,
                Bundle.class,
                Intent.class,
                Path.class,
                Matrix.class,
                RectF.class,
                Region.class,
                PorterDuff.class,
                Typeface.class,
                Uri.class,
                Canvas.class,
                Paint.class,
                Bitmap.class,
                MotionEvent.class,
                Color.class,
                net.pierrox.lightning_launcher.script.api.svg.SvgElement.class,
                net.pierrox.lightning_launcher.script.api.svg.SvgGroup.class,
                net.pierrox.lightning_launcher.script.api.svg.SvgPath.class,
                net.pierrox.lightning_launcher.script.api.svg.SvgSvg.class,
        };

        ArrayList<String> object_methods = new ArrayList<String>();
        Method[] methods = new Object().getClass().getMethods();
        for (Method m : methods) {
            object_methods.add(m.getName());
        }
        for (Class<?> cls : classes) {
            methods = cls.getDeclaredMethods();
            for (Method m : methods) {
                if (Modifier.isPublic(m.getModifiers())) {
                    boolean skip = false;
                    String name = m.getName();
                    if (name.equals("getEvent_")) {
                        name = "getEvent";
                    } else if (name.equals("getEventInternal") || object_methods.contains(name)) {
                        skip = true;
                    }
                    if (!skip) {

                        TokenMethod token = new TokenMethod(cls, name, m.getReturnType().getSimpleName(), m.getParameterTypes());
                        sAutoCompleteTokens.add(token);
                    }
                }
            }
        }
    }

    private int findTokenStart(CharSequence text, int cursor) {
        int i = cursor;

        while (i > 0 && Character.isLetterOrDigit(text.charAt(i - 1))) {
            i--;
        }

        return i;
    }

    private int findTokenEnd(CharSequence text, int cursor) {
        int i = cursor;
        int len = text.length();

        while (i < len) {
            if (!Character.isLetterOrDigit(text.charAt(i))) {
                return i;
            } else {
                i++;
            }
        }

        return len;
    }

    private void displayTokenApiReference(Token token) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(token.getApiReferenceLink()));
        startActivity(intent);
    }

    private void updateSuggestionsList(String token, String previousToken) {
        String preferred_class = null;
        if (previousToken != null) {
            for (TokenMethod t : sAutoCompleteTokens) {
                if (previousToken.equals(t.method)) {
                    preferred_class = t.return_cls;
                    break;
                }
            }
        }

        ArrayList<Token> preferred_completions = new ArrayList<>();
        ArrayList<Token> normal_completions = new ArrayList<>();
        for (TokenMethod t : sAutoCompleteTokens) {
            if (t.method_lower_case.startsWith(token)) {
                if (t.declaring_cls_name.equals(preferred_class)) {
                    preferred_completions.add(t);
                } else {
                    normal_completions.add(t);
                }
            }
        }

        // completions is an array of array : for each token name, there can be multiple suggestions (for instance the getWidth method can be defined for several classes)
        ArrayList<ArrayList<Token>> completions = new ArrayList<>();
        for (Token t : preferred_completions) {
            addTokenIfPossible(t, completions);

        }
        for (Token t : normal_completions) {
            addTokenIfPossible(t, completions);
        }

        mCompletionsViewGroup.removeAllViews();
        final LayoutInflater inflater = getLayoutInflater();
        if (completions.isEmpty()) {
            // add main Tokens if no others are available
            addSugestions(inflater, sMainTokens);
        }
        for (ArrayList<Token> tokens : completions) {
            addSugestion(inflater, tokens);
        }
    }

    private void addTokenIfPossible(Token pt, ArrayList<ArrayList<Token>> completions) {
        if (completions.size() == 20) return;
        String displayName = pt.getDisplayName();
        for (ArrayList<Token> tokens : completions) {
            if (tokens.get(0).getDisplayName().equals(displayName)) {
                tokens.add(pt);
                return;
            }
        }

        ArrayList<Token> l = new ArrayList<>(1);
        l.add(pt);
        completions.add(l);
    }

    private void addSugestions(LayoutInflater inflater, Token[] tokens) {
        for (Token token : tokens) {
            ArrayList<Token> l = new ArrayList<>(1);
            l.add(token);
            addSugestion(inflater, l);
        }
    }

    private void addSugestion(LayoutInflater inflater, ArrayList<Token> tokens) {
        Button b = (Button) inflater.inflate(R.layout.sc_btn, null);
        b.setTag(tokens);
        b.setText(tokens.get(0).getDisplayName());
        b.setOnClickListener(mCompletionButtonClickListener);
        b.setOnLongClickListener(mCompletionButtonLongClickListener);
        mCompletionsViewGroup.addView(b);
    }

    private void initializeShortcuts(ViewGroup view) {
        Typeface typeface = LLApp.get().getIconsTypeface();

        LayoutInflater inflater = getLayoutInflater();
        for (Shortcut shortcut : mShortcuts) {
            Button b = (Button) inflater.inflate(R.layout.sc_btn, null);
            b.setTag(shortcut);
            b.setText(shortcut.getLabel());
            if (shortcut.isLabelIcon())
                b.setTypeface(typeface);
            b.setOnClickListener(mShortcutButtonClickListener);
            view.addView(b);
        }
    }

    enum SA {
        DEC_TAB,
        INC_TAB,
        SEARCH,
        SEARCHN,
    }

    // -------------- shortcuts --------------------

    private interface Token {
        String LL_API_BASE_URL = "http://www.lightninglauncher.com/scripting/reference/api" + (BuildConfig.IS_BETA ? "-beta" : "") + "/reference/";
        String ANDROID_API_BASE_URL = "http://developer.android.com/reference/";

        String getDisplayName();

        String getUnambiguousDisplayName();

        String getApiReferenceLink();
    }

    /**
     * Represents a button in the bottom of the editor.
     */
    private interface Shortcut {
        /**
         * Label to display
         */
        String getLabel();

        /**
         * @return true if label is an icon (using LL typeface). False if normal text
         */
        boolean isLabelIcon();

        /**
         * Runned when the button is pressed
         */
        void apply(AdvancedEditText editText);
    }

    private static class TextPosition {
        int scrollX;
        int scrollY;
        int selectionStart;
        int selectionEnd;

        public TextPosition(AdvancedEditText t) {
            this.scrollX = t.getScrollX();
            this.scrollY = t.getScrollY();
            this.selectionStart = t.getSelectionStart();
            this.selectionEnd = t.getSelectionEnd();
        }

        public void apply(AdvancedEditText t) {
            try {
                t.setSelection(selectionStart, selectionEnd);
                t.scrollTo(scrollX, scrollY);
            } catch (Exception e) {
                // pass
                // scripts can be modified through scripts, hence selection indexes may not be in bounds anymore...
            }
        }
    }

    private static class TokenClass implements Token {
        Class cls;

        private TokenClass(Class cls) {
            this.cls = cls;
        }

        @Override
        public String getDisplayName() {
            return cls.getSimpleName();
        }

        @Override
        public String getUnambiguousDisplayName() {
            return cls.getSimpleName();
        }

        @Override
        public String getApiReferenceLink() {
            return (cls.getName().startsWith("android") ? ANDROID_API_BASE_URL : LL_API_BASE_URL) + cls.getName().replace('.', '/') + ".html";
        }
    }

    private static class TokenMethod implements Token {
        Class declaring_cls;
        String declaring_cls_name;
        String method;
        String method_lower_case;
        String return_cls;
        Class[] args;

        private TokenMethod(Class declaring_cls, String method, String return_cls, Class[] args) {
            this.declaring_cls = declaring_cls;
            this.declaring_cls_name = declaring_cls.getSimpleName();
            this.method = method;
            this.method_lower_case = method.toLowerCase();
            this.return_cls = return_cls;
            this.args = args;
        }

        @Override
        public String getDisplayName() {
            return method;
        }

        @Override
        public String getUnambiguousDisplayName() {
            String s = declaring_cls_name + "." + method + "(";
            int length = args.length;
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    s += ",";
                }
                s += args[i].getSimpleName();
            }
            s += ")";
            return s;
        }

        @Override
        public String getApiReferenceLink() {
            String s = (declaring_cls.getName().startsWith("android") ? ANDROID_API_BASE_URL : LL_API_BASE_URL) + declaring_cls.getName().replace('.', '/') + ".html#" + method + "(";
            int length = args.length;
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    s += ", ";
                }
                s += args[i].getName();
            }
            s += ")";
            return s;
        }
    }

    /**
     * This button will send a key to the editText.
     */
    private static class ShortcutKey implements Shortcut {
        private final int key;
        private final String label;

        ShortcutKey(int key, String iconLabel) {
            this.key = key;
            this.label = iconLabel;
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public boolean isLabelIcon() {
            return true;
        }

        @Override
        public void apply(AdvancedEditText editText) {
            editText.dispatchKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_DOWN,
                    key, 0));
            editText.dispatchKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_UP,
                    key, 0));
        }
    }

    /**
     * This button will insert a text before and after the cursor.
     */
    private static class ShortcutText implements Shortcut {
        private final String preText;
        private final String postText;

        ShortcutText(String preText, String postText) {
            this.preText = preText;
            this.postText = postText;
        }

        @Override
        public String getLabel() {
            return preText + "·" + postText;
        }

        @Override
        public boolean isLabelIcon() {
            return false;
        }

        @Override
        public void apply(AdvancedEditText editText) {
            int start = editText.getTrueSelectionStart();
            int end = editText.getTrueSelectionEnd();
            editText.getEditableText().replace(start, end, preText + postText);
            editText.setSelection(start + preText.length());
        }
    }

    /**
     * This button will trigger an action
     */
    private class ShortcutAction implements Shortcut {
        private final SA action;

        ShortcutAction(SA action) {
            this.action = action;
        }

        @Override
        public String getLabel() {
            switch (action) {
                case DEC_TAB:
                    return "A"; // left arrow with bar
                case INC_TAB:
                    return "D"; // right arrow with bar
                case SEARCH:
                    return "I"; // magnifier glass
                case SEARCHN:
                    return "IC"; // magnifier glass and '>'
            }
            return "n"; // android icon
        }

        @Override
        public boolean isLabelIcon() {
            return true;
        }

        @Override
        public void apply(AdvancedEditText editText) {
            switch (action) {
                case DEC_TAB:
                    Pair<Integer, Integer> selectionD =
                            Indentation.modifyIndent(editText.getTrueSelectionStart(), editText.getTrueSelectionEnd(), false, editText.getEditableText());
                    editText.setSelection(selectionD.first, selectionD.second);
                    break;
                case INC_TAB:
                    Pair<Integer, Integer> selectionI =
                            Indentation.modifyIndent(editText.getTrueSelectionStart(), editText.getTrueSelectionEnd(), true, editText.getEditableText());
                    editText.setSelection(selectionI.first, selectionI.second);
                    break;
                case SEARCH:
                    mSearch.showDialog();
                    break;
                case SEARCHN:
                    mSearch.searchNext();
            }
        }
    }
}
