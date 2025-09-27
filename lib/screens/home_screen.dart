import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/accessibility_service.dart';
import '../services/correction_service.dart';
import 'rules_screen.dart';

class HomeScreen extends StatefulWidget {
  @override
  _HomeScreenState createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  @override
  void initState() {
    super.initState();
    // Check permissions when app starts
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AccessibilityService>().checkPermissions();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Smart AutoCorrect'),
        backgroundColor: Colors.blue[600],
        foregroundColor: Colors.white,
      ),
      body: Consumer2<AccessibilityService, CorrectionService>(
        builder: (context, accessibilityService, correctionService, child) {
          return Padding(
            padding: EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _buildStatusCard(accessibilityService),
                SizedBox(height: 16),
                _buildQuickStats(correctionService),
                SizedBox(height: 16),
                _buildActionButtons(context, accessibilityService),
                SizedBox(height: 16),
                _buildInstructions(),
              ],
            ),
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          Navigator.push(
            context,
            MaterialPageRoute(builder: (context) => RulesScreen()),
          );
        },
        child: Icon(Icons.edit),
        tooltip: 'Manage Correction Rules',
      ),
    );
  }

  Widget _buildStatusCard(AccessibilityService service) {
    return Card(
      child: Padding(
        padding: EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Service Status',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            SizedBox(height: 12),
            _buildStatusRow(
              'Accessibility Service',
              service.isAccessibilityEnabled,
              'Allows reading text from other apps',
            ),
            SizedBox(height: 8),
            _buildStatusRow(
              'Overlay Permission',
              service.canDrawOverlays,
              'Allows showing correction suggestions',
            ),
            SizedBox(height: 12),
            Row(
              children: [
                Icon(
                  service.isFullyEnabled ? Icons.check_circle : Icons.warning,
                  color: service.isFullyEnabled ? Colors.green : Colors.orange,
                ),
                SizedBox(width: 8),
                Text(
                  service.isFullyEnabled 
                    ? 'Ready to use!' 
                    : 'Setup required',
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    color: service.isFullyEnabled ? Colors.green : Colors.orange,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatusRow(String title, bool enabled, String description) {
    return Row(
      children: [
        Icon(
          enabled ? Icons.check_circle : Icons.cancel,
          color: enabled ? Colors.green : Colors.red,
          size: 20,
        ),
        SizedBox(width: 8),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: TextStyle(fontWeight: FontWeight.w500)),
              Text(
                description,
                style: TextStyle(fontSize: 12, color: Colors.grey[600]),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildQuickStats(CorrectionService service) {
    return Card(
      child: Padding(
        padding: EdgeInsets.all(16.0),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          children: [
            _buildStatColumn('Total Rules', service.rules.length.toString()),
            _buildStatColumn('Active Rules', service.activeRules.length.toString()),
            _buildStatColumn('Disabled', (service.rules.length - service.activeRules.length).toString()),
          ],
        ),
      ),
    );
  }

  Widget _buildStatColumn(String label, String value) {
    return Column(
      children: [
        Text(
          value,
          style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.blue[600]),
        ),
        Text(
          label,
          style: TextStyle(fontSize: 12, color: Colors.grey[600]),
        ),
      ],
    );
  }

  Widget _buildActionButtons(BuildContext context, AccessibilityService service) {
    return Column(
      children: [
        if (!service.isAccessibilityEnabled)
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: () async {
                await service.openAccessibilitySettings();
                // Recheck permissions after returning from settings
                Future.delayed(Duration(seconds: 1), () {
                  service.checkPermissions();
                });
              },
              icon: Icon(Icons.accessibility),
              label: Text('Enable Accessibility Service'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.blue[600],
                foregroundColor: Colors.white,
                padding: EdgeInsets.symmetric(vertical: 12),
              ),
            ),
          ),
        if (!service.isAccessibilityEnabled) SizedBox(height: 8),
        
        if (!service.canDrawOverlays)
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: () async {
                await service.openOverlaySettings();
                // Recheck permissions after returning from settings
                Future.delayed(Duration(seconds: 1), () {
                  service.checkPermissions();
                });
              },
              icon: Icon(Icons.layers),
              label: Text('Allow Overlay Permission'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.orange[600],
                foregroundColor: Colors.white,
                padding: EdgeInsets.symmetric(vertical: 12),
              ),
            ),
          ),
        
        SizedBox(height: 8),
        SizedBox(
          width: double.infinity,
          child: ElevatedButton.icon(
            onPressed: () {
              service.checkPermissions();
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text('Status refreshed')),
              );
            },
            icon: Icon(Icons.refresh),
            label: Text('Refresh Status'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.grey[600],
              foregroundColor: Colors.white,
              padding: EdgeInsets.symmetric(vertical: 12),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildInstructions() {
    return Expanded(
      child: Card(
        child: Padding(
          padding: EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'How to Use',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              SizedBox(height: 12),
              _buildInstructionStep('1', 'Enable both permissions above'),
              _buildInstructionStep('2', 'Add or customize correction rules using the + button'),
              _buildInstructionStep('3', 'Start typing in any app - misspelled words will be detected'),
              _buildInstructionStep('4', 'Tap the correction suggestions to apply them'),
              SizedBox(height: 16),
              Text(
                'Note: The app works across all apps including WhatsApp, SMS, Email, and more!',
                style: TextStyle(
                  fontStyle: FontStyle.italic,
                  color: Colors.blue[600],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInstructionStep(String number, String text) {
    return Padding(
      padding: EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 24,
            height: 24,
            decoration: BoxDecoration(
              color: Colors.blue[600],
              borderRadius: BorderRadius.circular(12),
            ),
            child: Center(
              child: Text(
                number,
                style: TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold),
              ),
            ),
          ),
          SizedBox(width: 12),
          Expanded(
            child: Text(text),
          ),
        ],
      ),
    );
  }
}
