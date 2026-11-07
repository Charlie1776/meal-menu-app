# Meal Menu App
Single-screen daily meal planner for Android.
- Shows today's menu from today_menu.json (falls back to sample menu)
- Checkboxes mark meals eaten; struck through when checked
- State resets daily at midnight
- Place today's menu JSON at: app files dir / today_menu.json
Build with Android Studio or Codemagic (see codemagic.yaml).

## Cloud menu sync
- Menu format: `{"date":"YYYY-MM-DD","meals":[{"name","calories","protein","eaten"}]}` (legacy bare list still accepted).
- Tap **Menu URL** and paste an HTTPS URL to the raw JSON (e.g. a secret gist raw URL without a revision id:
  `https://gist.githubusercontent.com/<user>/<gist_id>/raw/today_menu.json`). No tokens are stored in the app.
- On open/resume (and **Refresh**) the app downloads, validates, then caches to `files/today_menu.json`; offline or invalid downloads keep the cached menu.
- Eaten checkboxes reset on a new day or when the menu date changes.
