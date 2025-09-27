import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';

class CorrectionRule {
  final String misspelled;
  final String correction;
  final bool isActive;

  CorrectionRule({
    required this.misspelled,
    required this.correction,
    this.isActive = true,
  });

  Map<String, dynamic> toJson() => {
    'misspelled': misspelled,
    'correction': correction,
    'isActive': isActive,
  };

  factory CorrectionRule.fromJson(Map<String, dynamic> json) => CorrectionRule(
    misspelled: json['misspelled'],
    correction: json['correction'],
    isActive: json['isActive'] ?? true,
  );
}

class CorrectionService extends ChangeNotifier {
  List<CorrectionRule> _rules = [];
  bool _autoCorrectEnabled = false;
  bool _overlayEnabled = true;
  bool _showOnFocus = true;
  String? _pendingMisspelled;
  
  List<CorrectionRule> get rules => _rules;
  List<CorrectionRule> get activeRules => _rules.where((rule) => rule.isActive).toList();
  bool get autoCorrectEnabled => _autoCorrectEnabled;
  bool get overlayEnabled => _overlayEnabled;
  bool get showOnFocus => _showOnFocus;
  String? get pendingMisspelled => _pendingMisspelled;

  CorrectionService() {
    _loadDefaultRules();
    _loadRules();
    _loadSettings();
  }

  void _loadDefaultRules() {
    _rules = [
      CorrectionRule(misspelled: 'teh', correction: 'the'),
      CorrectionRule(misspelled: 'recieve', correction: 'receive'),
      CorrectionRule(misspelled: 'seperate', correction: 'separate'),
      CorrectionRule(misspelled: 'definately', correction: 'definitely'),
      CorrectionRule(misspelled: 'occured', correction: 'occurred'),
      CorrectionRule(misspelled: 'necesary', correction: 'necessary'),
      CorrectionRule(misspelled: 'accomodate', correction: 'accommodate'),
      CorrectionRule(misspelled: 'acheive', correction: 'achieve'),
      CorrectionRule(misspelled: 'beleive', correction: 'believe'),
      CorrectionRule(misspelled: 'wierd', correction: 'weird'),
      CorrectionRule(misspelled: 'thier', correction: 'their'),
      CorrectionRule(misspelled: 'youre', correction: 'you\'re'),
      CorrectionRule(misspelled: 'its', correction: 'it\'s'),
      CorrectionRule(misspelled: 'dont', correction: 'don\'t'),
      CorrectionRule(misspelled: 'cant', correction: 'can\'t'),
    ];
  }

  Future<void> _loadRules() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final rulesJson = prefs.getString('correction_rules');
      if (rulesJson != null) {
        final List<dynamic> rulesList = json.decode(rulesJson);
        _rules = rulesList.map((rule) => CorrectionRule.fromJson(rule)).toList();
        notifyListeners();
      }
    } catch (e) {
      print('Error loading rules: $e');
    }
  }

  Future<void> _loadSettings() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      _autoCorrectEnabled = prefs.getBool('auto_correct_enabled') ?? false;
      _overlayEnabled = prefs.getBool('overlay_enabled') ?? true;
      _showOnFocus = prefs.getBool('show_on_focus') ?? true;
      _pendingMisspelled = prefs.getString('pending_misspelled');
      notifyListeners();
    } catch (e) {
      print('Error loading settings: $e');
    }
  }

  Future<void> _saveSettings() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('auto_correct_enabled', _autoCorrectEnabled);
      await prefs.setBool('overlay_enabled', _overlayEnabled);
      await prefs.setBool('show_on_focus', _showOnFocus);
      if (_pendingMisspelled == null) {
        await prefs.remove('pending_misspelled');
      } else {
        await prefs.setString('pending_misspelled', _pendingMisspelled!);
      }
    } catch (e) {
      print('Error saving settings: $e');
    }
  }

  Future<void> _saveRules() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final rulesJson = json.encode(_rules.map((rule) => rule.toJson()).toList());
      await prefs.setString('correction_rules', rulesJson);
    } catch (e) {
      print('Error saving rules: $e');
    }
  }

  void addRule(String misspelled, String correction) {
    // Check if rule already exists
    final existingIndex = _rules.indexWhere((rule) => 
        rule.misspelled.toLowerCase() == misspelled.toLowerCase());
    
    if (existingIndex != -1) {
      // Update existing rule
      _rules[existingIndex] = CorrectionRule(
        misspelled: misspelled,
        correction: correction,
        isActive: true,
      );
    } else {
      // Add new rule
      _rules.add(CorrectionRule(
        misspelled: misspelled,
        correction: correction,
      ));
    }
    
    _saveRules();
    notifyListeners();
  }

  void setPendingMisspelled(String? value) {
    _pendingMisspelled = value;
    _saveSettings();
    notifyListeners();
  }

  void setAutoCorrectEnabled(bool value) {
    _autoCorrectEnabled = value;
    _saveSettings();
    notifyListeners();
  }

  void setOverlayEnabled(bool value) {
    _overlayEnabled = value;
    _saveSettings();
    notifyListeners();
  }

  void setShowOnFocus(bool value) {
    _showOnFocus = value;
    _saveSettings();
    notifyListeners();
  }

  void removeRule(int index) {
    if (index >= 0 && index < _rules.length) {
      _rules.removeAt(index);
      _saveRules();
      notifyListeners();
    }
  }

  void toggleRule(int index) {
    if (index >= 0 && index < _rules.length) {
      final rule = _rules[index];
      _rules[index] = CorrectionRule(
        misspelled: rule.misspelled,
        correction: rule.correction,
        isActive: !rule.isActive,
      );
      _saveRules();
      notifyListeners();
    }
  }

  String? getCorrection(String word) {
    final cleanWord = word.toLowerCase().replaceAll(RegExp(r'[^\p{L}]', unicode: true), '');
    final rule = activeRules.firstWhere(
      (rule) => rule.misspelled.toLowerCase() == cleanWord,
      orElse: () => CorrectionRule(misspelled: '', correction: ''),
    );
    return rule.misspelled.isNotEmpty ? rule.correction : null;
  }
}
