# Smart AutoCorrect

An intelligent autocorrection tool for Android that works across all apps using accessibility services. It detects misspelled words you define and offers corrections with a beautiful overlay interface.

## Features

- **Universal Correction**: Works in any app (WhatsApp, SMS, Email, browsers, etc.)
- **Custom Rules**: Add your own misspelled words and corrections
- **Smart Detection**: Real-time text analysis as you type
- **Beautiful UI**: Modern Material Design interface
- **Overlay Suggestions**: Non-intrusive correction suggestions
- **Toggle Rules**: Enable/disable specific correction rules
- **Persistent Storage**: Your rules are saved between app sessions

## How It Works

1. **Accessibility Service**: Monitors text input across all apps
2. **Text Analysis**: Detects misspelled words based on your custom rules
3. **Overlay Display**: Shows correction suggestions in a floating window
4. **One-Click Apply**: Tap to apply corrections instantly

## Installation & Setup

### Prerequisites
- Android 6.0 (API level 23) or higher
- Flutter 3.10.0 or higher

### Build Instructions

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd smart_autocorrect
   ```

2. **Install dependencies**:
   ```bash
   flutter pub get
   ```

3. **Build and install**:
   ```bash
   flutter build apk
   flutter install
   ```

### First-Time Setup

1. **Enable Accessibility Service**:
   - Open the app
   - Tap "Enable Accessibility Service"
   - Find "Smart AutoCorrect" in the accessibility settings
   - Turn it ON

2. **Allow Overlay Permission**:
   - Tap "Allow Overlay Permission" in the app
   - Enable "Display over other apps"

3. **Add Correction Rules**:
   - Tap the "+" button
   - Add your misspelled words and their corrections
   - Example: "teh" → "the"

## Usage

1. Start typing in any app
2. When a misspelled word is detected, a correction overlay appears
3. Tap "Apply" to use the correction
4. Tap "Dismiss" to ignore the suggestion

## Default Correction Rules

The app comes with common correction rules:
- teh → the
- recieve → receive
- seperate → separate
- definately → definitely
- occured → occurred
- necesary → necessary
- accomodate → accommodate
- acheive → achieve
- beleive → believe
- wierd → weird
- thier → their
- youre → you're
- its → it's
- dont → don't
- cant → can't

## Technical Architecture

### Flutter App Structure
- `lib/main.dart` - App entry point
- `lib/services/` - Business logic services
- `lib/screens/` - UI screens
- `android/app/src/main/kotlin/` - Native Android code

### Android Components
- **MainActivity.kt** - Flutter-Android bridge
- **AutoCorrectAccessibilityService.kt** - Text monitoring service
- **OverlayService.kt** - Floating UI management

### Key Technologies
- **Flutter** - Cross-platform UI framework
- **Accessibility Service** - System-level text access
- **Overlay Windows** - Floating UI elements
- **SharedPreferences** - Local data storage

## Permissions

- `BIND_ACCESSIBILITY_SERVICE` - Read text from other apps
- `SYSTEM_ALERT_WINDOW` - Display overlay windows
- `FOREGROUND_SERVICE` - Background processing
- `WAKE_LOCK` - Keep service active

## Troubleshooting

### Overlay Not Appearing
- Ensure overlay permission is granted
- Check that accessibility service is enabled
- Restart the app after enabling permissions

### Not Detecting Text
- Verify accessibility service is running
- Some apps may block accessibility access
- Try typing in a different app (like SMS or browser)

### App Crashes
- Check Android version compatibility (requires API 23+)
- Restart device if accessibility service becomes unresponsive

## Privacy & Security

- **No Data Collection**: All processing is done locally
- **No Internet Required**: Works completely offline
- **No Text Logging**: Only correction rules are stored
- **Open Source**: Full source code available for review

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly on different Android versions
5. Submit a pull request

## License

This project is open source. Feel free to use, modify, and distribute according to your needs.

## Support

For issues or questions:
1. Check the troubleshooting section
2. Create an issue in the repository
3. Ensure you're using a supported Android version

---

**Note**: This app requires accessibility permissions to function. It's designed to be privacy-focused and works entirely offline.
