import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

class AccessibilityService extends ChangeNotifier {
  static const platform = MethodChannel('smart_autocorrect/accessibility');
  
  bool _isAccessibilityEnabled = false;
  bool _canDrawOverlays = false;
  
  bool get isAccessibilityEnabled => _isAccessibilityEnabled;
  bool get canDrawOverlays => _canDrawOverlays;
  bool get isFullyEnabled => _isAccessibilityEnabled && _canDrawOverlays;

  Future<void> checkPermissions() async {
    try {
      _isAccessibilityEnabled = await platform.invokeMethod('isAccessibilityServiceEnabled');
      _canDrawOverlays = await platform.invokeMethod('canDrawOverlays');
      notifyListeners();
    } catch (e) {
      print('Error checking permissions: $e');
    }
  }

  Future<void> openAccessibilitySettings() async {
    try {
      await platform.invokeMethod('openAccessibilitySettings');
    } catch (e) {
      print('Error opening accessibility settings: $e');
    }
  }

  Future<void> openOverlaySettings() async {
    try {
      await platform.invokeMethod('openOverlaySettings');
    } catch (e) {
      print('Error opening overlay settings: $e');
    }
  }
}
