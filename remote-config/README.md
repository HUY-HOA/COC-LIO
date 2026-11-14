# COC-LIO remote UI configuration

This folder contains the JSON schema for the Firebase Remote Config parameter used by the Android app.

## One-time Firebase setup

1. Open Firebase project `coc-lio-73de9`.
2. Open **Remote Config**.
3. Create a String parameter named `coclio_app_config_json`.
4. Set its default value to the complete contents of `app.json`.
5. Publish the parameter.
6. In Google Cloud APIs, ensure **Firebase Remote Config Realtime API** is enabled so installed clients can receive updates while running.

The APK includes matching local defaults. The first APK that contains this remote-config client must be built and installed once. After that, supported configuration updates can be published without rebuilding the APK.

## Remotely configurable UI

- App title and subtitle.
- Light and dark theme colors.
- LED color palette and animation speed multiplier.
- Bottom navigation order, labels and visibility.
- Feature switches for navigation pages, the JSON import shortcut and remote UI blocks.
- Remote text, notices, cards, dividers, spacers, buttons, rows and columns on individual pages or all pages. Rows and columns can contain nested blocks, allowing the order and grouping of remote panels to be rearranged.
- Existing Vietnamese and English strings through the `translations.vi` and `translations.en` dictionaries.

### Remote UI blocks

Set `dashboard.blocks` to an ordered array. Each block can use one of these types:

- `text`: short text line.
- `notice` or `card`: title, message and optional action button.
- `button`: a full-width action button.
- `divider`: a separator.
- `spacer`: vertical spacing.
- `row` and `column`: layout containers with a `children` array of nested blocks. Row children can set `weight` to control their relative widths. Nesting is limited to three levels and five children per container.
- `image`: an HTTPS image with optional caption and `imageHeightDp` (60–300).
- `list`: a short list using the `items` string array (up to eight entries).
- `metric`: a large value in `message` with a label in `title`.
- `badge`: a compact status label using the `title` or `message` and `severity`.
- `link`: an external HTTPS link using `url` and `actionLabel` (or `title`).

Remote images and links are restricted to HTTPS URLs. Dashboard content is data-only; it cannot supply scripts, arbitrary actions or executable code.

Use `pageId` values `home`, `buildings`, `history`, `community`, `settings`, `lookup` or `all`. Available action IDs are allowlisted in the APK: `import_json`, `open_game`, `open_settings`, `open_community`, `open_history`, and `open_lookup`. The app ignores unknown block types, page IDs and actions.

Example: a notice on every page and two side-by-side quick actions on the overview page:

```json
{
  "dashboard": {
    "blocks": [
      {
        "id": "service-notice",
        "pageId": "all",
        "type": "notice",
        "title": "Thông báo",
        "message": "Nội dung thông báo được thay đổi từ máy chủ.",
        "severity": "info"
      },
      {
        "id": "home-quick-actions",
        "pageId": "home",
        "type": "row",
        "children": [
          {
            "type": "button",
            "actionId": "open_game",
            "actionLabel": "MỞ GAME",
            "weight": 1
          },
          {
            "type": "button",
            "actionId": "import_json",
            "actionLabel": "NHẬP JSON",
            "weight": 1
          }
        ]
      }
    ]
  }
}
```

### Publishing future changes

1. Edit `app.json` in this repository.
2. Copy the complete JSON into the Firebase parameter `coclio_app_config_json`.
3. Increase the `version` value.
4. Publish the Remote Config changes.

With Realtime updates enabled, an app that is open and connected can receive a published update without waiting for the next fetch interval. If Realtime updates are unavailable, the app uses the last activated/default configuration and fetches again when it is next initialized. The production fallback fetch interval is 12 hours.

## What still requires a new APK

Remote Config only changes values and UI structures that are already supported by the installed app. New Kotlin/Java code, new screen logic, new data parsing algorithms, new API integrations, database schema changes that require client code, Android permissions, notification/background services, home-screen widgets, native libraries, sign-in flows and security/privacy enforcement still require an APK build.

Remote configuration must never contain passwords, private keys, access tokens, personal data, executable scripts or downloaded code. Remote UI actions are limited to the explicit allowlist compiled into the app. Do not use Remote Config to bypass Android permission prompts or user consent.
