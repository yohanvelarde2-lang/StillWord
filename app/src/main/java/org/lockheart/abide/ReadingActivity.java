package org.lockheart.abide;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ReadingActivity extends Activity {
  static final String KINDLE = "kindle";
  static final String IMPORT = "book-import";
  static final String LIBRARY = "book-library";
  private static final String BOOK = "book";
  private static final int PICK_BOOK = 31;
  private static final int INK = Color.rgb(32, 60, 51);
  private static final int GREEN = Color.rgb(52, 93, 80);
  private static final int PAPER = Color.rgb(246, 244, 236);
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private Store store;
  private BookFiles files;
  private Bible bible;
  private LinearLayout root, body;
  private TextView timer, bookStatus;
  private ScrollView bookScroll;
  private float initialScroll;
  private Button toggle, release;
  private String mode;
  private BookContent book;
  private boolean saved, resumed, focused, paused, contentReady, picking;
  private int page, generation;
  private long lastTick, activeCycle, lastSave;

  static Intent intent(Context context, String mode) {
    return new Intent(context, ReadingActivity.class).putExtra("mode", mode);
  }

  @Override protected void onCreate(Bundle state) {
    super.onCreate(state);
    store = Store.get(this);
    files = new BookFiles(this);
    bible = Bible.get(this);
    mode = state == null ? getIntent().getStringExtra("mode") : state.getString("mode");
    if (state != null) {
      paused = state.getBoolean("paused");
      picking = state.getBoolean("picking");
    }
    activeCycle = store.policy.cycleStart;
    if (KINDLE.equals(mode)) showKindle();
    else if (LIBRARY.equals(mode)) showLibrary();
    else if (BOOK.equals(mode) && state != null) {
      boolean wasSaved = state.getBoolean("saved");
      File file = files.restore(state.getString("file"), wasSaved);
      if (file == null) showLibrary();
      else loadBook(file, wasSaved, state.getInt("page"), state.getFloat("scroll"));
    } else {
      showImport();
      if (state == null) pickBook();
    }
    lastTick = SystemClock.elapsedRealtime();
  }

  @Override protected void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    state.putString("mode", mode);
    state.putBoolean("paused", paused);
    state.putBoolean("picking", picking);
    if (book != null) {
      state.putString("file", book.file.getName());
      state.putBoolean("saved", saved);
      state.putInt("page", page);
      state.putFloat("scroll", readingPosition());
    }
  }

  @Override protected void onResume() {
    super.onResume();
    resumed = true;
    lastTick = SystemClock.elapsedRealtime();
    setBookFullscreen(bookScroll != null);
    // Battery: the timer runs only while this screen is in front.
    handler.removeCallbacks(tick);
    handler.post(tick);
    if (GuardService.instance != null) GuardService.instance.requestCheck();
  }

  @Override protected void onPause() {
    resumed = false;
    handler.removeCallbacks(tick);
    rememberPage();
    store.save();
    super.onPause();
  }

  @Override public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    focused = hasFocus;
    lastTick = SystemClock.elapsedRealtime();
    if (hasFocus && bookScroll != null) setBookFullscreen(true);
  }

  @Override public void onConfigurationChanged(Configuration configuration) {
    super.onConfigurationChanged(configuration);
    if (BOOK.equals(mode) && book != null && (saved || book.pages == null)) {
      showPage(page, readingPosition());
    }
  }

  @Override public void onBackPressed() {
    if (BOOK.equals(mode) && saved) showLibrary();
    else finish();
  }

  @Override protected void onDestroy() {
    generation++;
    handler.removeCallbacksAndMessages(null);
    worker.shutdownNow();
    if (isFinishing()) discardBook();
    super.onDestroy();
  }

  private final Runnable tick = new Runnable() {
    @Override public void run() {
      long now = SystemClock.elapsedRealtime();
      long delta = Math.min(500, Math.max(0, now - lastTick));
      lastTick = now;
      store.refresh();
      long cycle = store.policy.cycleStart;
      boolean active = contentReady && resumed && focused && !paused
          && getSystemService(PowerManager.class).isInteractive()
          && !getSystemService(KeyguardManager.class).isKeyguardLocked();
      if (active && activeCycle == cycle && !store.policy.ready()) {
        boolean complete = store.policy.credit(delta, System.currentTimeMillis());
        if (complete || now - lastSave >= 5000) {
          store.save();
          lastSave = now;
        }
        if (complete) MainActivity.notifyReady(ReadingActivity.this);
      }
      activeCycle = store.policy.cycleStart;
      updateTimer(active);
      handler.postDelayed(this, 250);
    }
  };

  private void updateTimer(boolean active) {
    if (timer == null && bookStatus == null) return;
    long seconds = (Math.max(0, store.policy.goal() - store.policy.readMs) + 999) / 1000;
    String time = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    String caption = store.policy.ready() ? "Reading goal complete"
        : active ? "remaining" : "remaining · paused";
    if (bookStatus != null) {
      String status = (book.pages == null ? "Page " : "Section ") + (page + 1) + " / " + book.count
          + "   ·   " + (store.policy.ready() ? "Goal complete" : time + (paused ? " paused" : " left"));
      if (!status.contentEquals(bookStatus.getText())) bookStatus.setText(status);
      return;
    }
    timer.setText(time + "  •  " + caption);
    toggle.setText(paused ? "Resume timer" : "Pause timer");
    toggle.setVisibility(store.policy.ready() ? Button.GONE : Button.VISIBLE);
    release.setVisibility(store.policy.ready() && !store.policy.unlocked ? Button.VISIBLE : Button.GONE);
  }

  private void showKindle() {
    mode = KINDLE;
    makeScreen("READ FROM KINDLE", "A quiet reading moment");
    text(body, "Keep this screen open while you read on your Kindle.", 17, false);
    text(body, "“" + bible.verse("PHP", 4, "8") + "”", 23, true);
    text(body, "Philippians 4:8 · " + bible.active.optString("abbreviation"), 14, false);
    text(body, "Your timer pauses when you switch apps or lock your phone.", 14, false);
    button(body, "End Kindle session", this::finish);
    contentReady = true;
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    updateTimer(false);
  }

  private void showImport() {
    mode = IMPORT;
    makeScreen("YOUR BOOKS", "Upload a book");
    text(body, "Choose a PDF, EPUB or MOBI file from your phone. You can read it once or save a copy to Download Library for offline reading.", 17, false);
    button(body, "Choose a file", this::pickBook);
    button(body, "Download Library", this::showLibrary);
  }

  private void pickBook() {
    if (picking) return;
    // File providers do not all report EPUB and MOBI MIME types consistently.
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
        .setType("*/*");
    try {
      startActivityForResult(intent, PICK_BOOK);
      picking = true;
    } catch (android.content.ActivityNotFoundException e) {
      toast("Android's file picker is unavailable.");
    }
  }

  @Override protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request != PICK_BOOK) return;
    picking = false;
    if (result != RESULT_OK || data == null || data.getData() == null) return;
    showLoading();
    int job = ++generation;
    worker.execute(() -> {
      File file = null;
      try {
        file = files.importFile(getContentResolver(), data.getData());
        BookContent content = BookContent.open(file);
        runOnUiThread(() -> {
          if (job == generation && !isFinishing() && !isDestroyed()) showBook(content, false, 0, 0);
          else content.file.delete();
        });
      } catch (Exception e) {
        if (file != null) file.delete();
        showBookError(job, e, false);
      }
    });
  }

  private void showLibrary() {
    generation++;
    rememberPage();
    discardBook();
    mode = LIBRARY;
    makeScreen("YOUR BOOKS", "Download Library");
    text(body, "Saved copies are available offline on this phone.", 16, false);
    File[] entries = files.list();
    if (entries.length == 0) text(body, "Your library is empty. Upload a book, then tap Save to Download Library.", 16, false);
    for (File file : entries) {
      button(body, BookFiles.title(file), () -> {
        BookBookmark bookmark = BookBookmark.load(store.prefs, file.getName());
        loadBook(file, true, bookmark.page, bookmark.scroll);
      });
    }
    button(body, "Upload a book", () -> { showImport(); pickBook(); });
    button(body, "Back", this::finish);
  }

  private void loadBook(File file, boolean fromLibrary, int index, float position) {
    showLoading();
    int job = ++generation;
    worker.execute(() -> {
      try {
        BookContent content = BookContent.open(file);
        runOnUiThread(() -> {
          if (job == generation && !isFinishing() && !isDestroyed()) showBook(content, fromLibrary, index, position);
        });
      } catch (Exception e) {
        if (!fromLibrary) file.delete();
        showBookError(job, e, fromLibrary);
      }
    });
  }

  private void showLoading() {
    makeScreen("YOUR BOOKS", "Opening your book…");
    text(body, "Preparing your local copy.", 16, false);
  }

  private void showBookError(int job, Exception error, boolean fromLibrary) {
    runOnUiThread(() -> {
      if (job != generation || isFinishing() || isDestroyed()) return;
      if (fromLibrary) showLibrary(); else showImport();
      new android.app.AlertDialog.Builder(this).setTitle("Could not open this book")
          .setMessage(error instanceof IOException ? error.getMessage() : "The file is damaged or uses an unsupported format.")
          .setPositiveButton("OK", null).show();
    });
  }

  private void showBook(BookContent content, boolean fromLibrary, int index, float position) {
    mode = BOOK;
    book = content;
    saved = fromLibrary;
    int restoredPage = Math.max(0, Math.min(content.count - 1, index));
    showPage(restoredPage, restoredPage == index ? position : 0);
  }

  private void turnPage(int index) {
    rememberPage();
    showPage(index, 0);
  }

  private void showPage(int index, float position) {
    if (index < 0 || index >= book.count) return;
    page = index;
    initialScroll = position;
    if (saved) {
      makeBookScreen();
    } else {
      makeScreen("OFFLINE READER", BookFiles.title(book.file));
      text(body, (book.pages == null ? "Page " : "Section ") + (page + 1) + " of " + book.count, 15, false);
      LinearLayout actions = new LinearLayout(this);
      actions.setGravity(Gravity.CENTER);
      body.addView(actions);
      button(actions, "Previous", () -> turnPage(page - 1)).setEnabled(page > 0);
      button(actions, "Next", () -> turnPage(page + 1)).setEnabled(page + 1 < book.count);
      button(body, "Save to Download Library", this::saveBook);
    }
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    if (book.pages != null) {
      TextView words = text(body, book.pages.get(page), 21, true);
      if (saved) words.setPadding(dp(14), dp(8), dp(14), dp(12));
      words.setTextIsSelectable(true);
      pageReady();
    } else {
      ImageView image = new ImageView(this);
      image.setAdjustViewBounds(true);
      image.setContentDescription("PDF page " + (page + 1));
      body.addView(image, new LinearLayout.LayoutParams(-1, -2));
      BookContent content = book;
      int job = ++generation;
      int width = Math.max(800, getResources().getDisplayMetrics().widthPixels * 2);
      worker.execute(() -> {
        try {
          Bitmap bitmap = content.renderPdfPage(index, width);
          runOnUiThread(() -> {
            if (job != generation || isFinishing() || isDestroyed()) { bitmap.recycle(); return; }
            image.setImageBitmap(bitmap);
            pageReady();
          });
        } catch (Exception e) {
          runOnUiThread(() -> {
            if (job == generation && !isFinishing() && !isDestroyed()) {
              image.setVisibility(ImageView.GONE);
              text(body, "This PDF page could not be displayed.", 16, false);
            }
          });
        }
      });
    }
    if (!saved) {
      LinearLayout footer = new LinearLayout(this);
      footer.setGravity(Gravity.CENTER);
      body.addView(footer);
      if (page > 0) button(footer, "Previous", () -> turnPage(page - 1));
      if (page + 1 < book.count) button(footer, "Next", () -> turnPage(page + 1));
    }
    updateTimer(false);
  }

  private void saveBook() {
    try {
      float position = readingPosition();
      File file = files.save(book.file);
      book = book.withFile(file);
      saved = true;
      new BookBookmark(page, position).save(store.prefs, file.getName());
      showPage(page, position);
      toast("Book saved for offline reading.");
    } catch (IOException e) {
      toast(e.getMessage());
    }
  }

  private void pageReady() {
    contentReady = true;
    if (bookScroll != null && initialScroll > 0) {
      ScrollView scroll = bookScroll;
      scroll.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
        @Override public boolean onPreDraw() {
          scroll.getViewTreeObserver().removeOnPreDrawListener(this);
          if (scroll == bookScroll) {
            scroll.scrollTo(0, Math.round(initialScroll * scrollRange(scroll)));
            initialScroll = 0;
          }
          return true;
        }
      });
    }
    rememberPage();
  }

  private float readingPosition() {
    if (initialScroll > 0) return initialScroll;
    if (!BOOK.equals(mode) || !(body.getParent() instanceof ScrollView)) return 0;
    ScrollView scroll = (ScrollView) body.getParent();
    int range = scrollRange(scroll);
    return range == 0 ? 0 : Math.min(1, (float) scroll.getScrollY() / range);
  }

  private int scrollRange(ScrollView scroll) {
    return Math.max(0, scroll.getChildAt(0).getHeight() - scroll.getHeight());
  }

  private void rememberPage() {
    if (book == null || !saved || !contentReady) return;
    new BookBookmark(page, readingPosition()).save(store.prefs, book.file.getName());
  }

  private void openMain(String destination) {
    rememberPage();
    startActivity(new Intent(this, MainActivity.class).putExtra("page", destination));
    finish();
  }

  private void showBookTimer() {
    AlertDialog.Builder dialog = new AlertDialog.Builder(this)
        .setTitle("Reading goal")
        .setMessage(bookStatus.getText())
        .setNegativeButton("Back to book", null);
    if (!store.policy.ready()) {
      dialog.setPositiveButton(paused ? "Resume timer" : "Pause timer", (window, choice) -> {
        paused = !paused;
        lastTick = SystemClock.elapsedRealtime();
        updateTimer(false);
      });
    } else if (!store.policy.unlocked) {
      dialog.setPositiveButton("View John 8:32 & unlock", (window, choice) -> openMain("release"));
    }
    dialog.show();
  }

  private void makeBookScreen() {
    contentReady = false;
    timer = null;
    root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(PAPER);
    root.setOnApplyWindowInsetsListener((view, insets) -> {
      if (Build.VERSION.SDK_INT >= 28 && insets.getDisplayCutout() != null) {
        android.view.DisplayCutout cutout = insets.getDisplayCutout();
        view.setPadding(cutout.getSafeInsetLeft(), cutout.getSafeInsetTop(),
            cutout.getSafeInsetRight(), cutout.getSafeInsetBottom());
      } else {
        view.setPadding(0, 0, 0, 0);
      }
      return insets;
    });
    bookScroll = new ScrollView(this);
    bookScroll.setFillViewport(true);
    root.addView(bookScroll, new LinearLayout.LayoutParams(-1, 0, 1));
    body = new LinearLayout(this);
    body.setOrientation(LinearLayout.VERTICAL);
    bookScroll.addView(body);

    bookStatus = new TextView(this);
    bookStatus.setTextColor(INK);
    bookStatus.setTextSize(13);
    bookStatus.setGravity(Gravity.CENTER);
    bookStatus.setMinHeight(dp(32));
    bookStatus.setFocusable(true);
    bookStatus.setOnClickListener(v -> showBookTimer());
    root.addView(bookStatus, new LinearLayout.LayoutParams(-1, -2));

    LinearLayout controls = new LinearLayout(this);
    controls.setBackgroundColor(Color.rgb(232, 236, 226));
    root.addView(controls, new LinearLayout.LayoutParams(-1, -2));
    bookControl(controls, "Previous", () -> turnPage(page - 1)).setEnabled(page > 0);
    bookControl(controls, "Next", () -> turnPage(page + 1)).setEnabled(page + 1 < book.count);
    bookControl(controls, "Close", this::showLibrary);
    bookControl(controls, "Home", () -> openMain("home"));
    setContentView(root);
    setBookFullscreen(true);
  }

  private Button bookControl(LinearLayout row, String label, Runnable action) {
    Button control = new Button(this, null, android.R.attr.borderlessButtonStyle);
    control.setText(label);
    control.setAllCaps(false);
    control.setTextColor(new android.content.res.ColorStateList(
        new int[][] {new int[] {-android.R.attr.state_enabled}, new int[] {}},
        new int[] {Color.rgb(115, 122, 117), INK}));
    control.setTextSize(13);
    control.setPadding(dp(2), dp(4), dp(2), dp(4));
    control.setMinHeight(dp(48));
    control.setMinimumWidth(0);
    control.setMinWidth(0);
    row.addView(control, new LinearLayout.LayoutParams(0, -2, 1));
    control.setOnClickListener(v -> action.run());
    return control;
  }

  @SuppressWarnings("deprecation")
  private void setBookFullscreen(boolean full) {
    if (Build.VERSION.SDK_INT >= 30) {
      WindowInsetsController controller = getWindow().getInsetsController();
      if (controller == null) return;
      if (full) {
        controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsets.Type.systemBars());
      } else {
        controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_DEFAULT);
        controller.show(WindowInsets.Type.systemBars());
      }
    } else {
      getWindow().getDecorView().setSystemUiVisibility(full
          ? View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY : 0);
    }
  }

  private void discardBook() {
    if (book != null && !saved) book.file.delete();
    book = null;
    saved = false;
  }

  private void makeScreen(String eyebrow, String title) {
    contentReady = false;
    bookScroll = null;
    bookStatus = null;
    initialScroll = 0;
    setBookFullscreen(false);
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(PAPER);
    root.setPadding(dp(22), dp(10), dp(22), 0);
    if (android.os.Build.VERSION.SDK_INT >= 35) {
      root.setOnApplyWindowInsetsListener((view, insets) -> {
        android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        view.setPadding(dp(22) + bars.left, dp(10) + bars.top, dp(22) + bars.right, bars.bottom);
        return insets;
      });
    }
    setContentView(root);
    timer = null;
    if (KINDLE.equals(mode) || BOOK.equals(mode)) {
      timer = text(root, "", 19, false);
      timer.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
      LinearLayout row = new LinearLayout(this);
      row.setGravity(Gravity.START);
      root.addView(row);
      toggle = button(row, "Pause timer", () -> { paused = !paused; lastTick = SystemClock.elapsedRealtime(); updateTimer(false); });
      release = button(row, "View John 8:32 & unlock", () -> openMain("release"));
    }
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    body = new LinearLayout(this);
    body.setOrientation(LinearLayout.VERTICAL);
    body.setPadding(0, dp(8), 0, dp(20));
    scroll.addView(body);
    text(body, eyebrow, 13, false).setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    text(body, title, 30, true);
  }

  private TextView text(LinearLayout parent, String value, int size, boolean serif) {
    TextView view = new TextView(this);
    view.setText(value);
    view.setTextSize(size);
    view.setTextColor(INK);
    view.setLineSpacing(dp(5), 1);
    view.setPadding(0, dp(7), 0, dp(7));
    if (serif) view.setTypeface(Typeface.SERIF);
    parent.addView(view, new LinearLayout.LayoutParams(-1, -2));
    return view;
  }

  private Button button(LinearLayout parent, String label, Runnable action) {
    Button view = new Button(this);
    view.setText(label);
    view.setAllCaps(false);
    view.setTextColor(Color.WHITE);
    view.setPadding(dp(16), dp(8), dp(16), dp(8));
    GradientDrawable background = new GradientDrawable();
    background.setColor(GREEN);
    background.setCornerRadius(dp(16));
    view.setBackground(background);
    LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(
        parent.getOrientation() == LinearLayout.VERTICAL ? -1 : -2, -2);
    layout.setMargins(0, dp(6), dp(4), dp(4));
    parent.addView(view, layout);
    view.setOnClickListener(v -> action.run());
    return view;
  }

  private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
  private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
}
