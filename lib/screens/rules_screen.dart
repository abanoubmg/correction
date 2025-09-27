import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/correction_service.dart';

class RulesScreen extends StatefulWidget {
  @override
  _RulesScreenState createState() => _RulesScreenState();
}

class _RulesScreenState extends State<RulesScreen> {
  final _misspelledController = TextEditingController();
  final _correctionController = TextEditingController();

  @override
  void dispose() {
    _misspelledController.dispose();
    _correctionController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Correction Rules'),
        backgroundColor: Colors.blue[600],
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: Icon(Icons.add),
            onPressed: () => _showAddRuleDialog(),
          ),
        ],
      ),
      body: Consumer<CorrectionService>(
        builder: (context, correctionService, child) {
          final rules = correctionService.rules;
          
          if (rules.isEmpty) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.spellcheck, size: 64, color: Colors.grey),
                  SizedBox(height: 16),
                  Text(
                    'No correction rules yet',
                    style: TextStyle(fontSize: 18, color: Colors.grey),
                  ),
                  SizedBox(height: 8),
                  Text(
                    'Tap the + button to add your first rule',
                    style: TextStyle(color: Colors.grey),
                  ),
                ],
              ),
            );
          }

          return ListView.builder(
            padding: EdgeInsets.all(16),
            itemCount: rules.length,
            itemBuilder: (context, index) {
              final rule = rules[index];
              return Card(
                margin: EdgeInsets.only(bottom: 8),
                child: ListTile(
                  leading: Checkbox(
                    value: rule.isActive,
                    onChanged: (value) {
                      correctionService.toggleRule(index);
                    },
                  ),
                  title: Text(
                    '${rule.misspelled} → ${rule.correction}',
                    style: TextStyle(
                      decoration: rule.isActive ? null : TextDecoration.lineThrough,
                      color: rule.isActive ? null : Colors.grey,
                    ),
                  ),
                  subtitle: Text(
                    rule.isActive ? 'Active' : 'Disabled',
                    style: TextStyle(
                      color: rule.isActive ? Colors.green : Colors.grey,
                      fontSize: 12,
                    ),
                  ),
                  trailing: PopupMenuButton(
                    itemBuilder: (context) => [
                      PopupMenuItem(
                        value: 'edit',
                        child: Row(
                          children: [
                            Icon(Icons.edit, size: 18),
                            SizedBox(width: 8),
                            Text('Edit'),
                          ],
                        ),
                      ),
                      PopupMenuItem(
                        value: 'delete',
                        child: Row(
                          children: [
                            Icon(Icons.delete, size: 18, color: Colors.red),
                            SizedBox(width: 8),
                            Text('Delete', style: TextStyle(color: Colors.red)),
                          ],
                        ),
                      ),
                    ],
                    onSelected: (value) {
                      if (value == 'edit') {
                        _showEditRuleDialog(rule, index);
                      } else if (value == 'delete') {
                        _showDeleteConfirmation(index);
                      }
                    },
                  ),
                ),
              );
            },
          );
        },
      ),
    );
  }

  void _showAddRuleDialog() {
    _misspelledController.clear();
    _correctionController.clear();
    
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Add Correction Rule'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: _misspelledController,
              decoration: InputDecoration(
                labelText: 'Misspelled word',
                hintText: 'e.g., teh',
                border: OutlineInputBorder(),
              ),
              textCapitalization: TextCapitalization.none,
            ),
            SizedBox(height: 16),
            TextField(
              controller: _correctionController,
              decoration: InputDecoration(
                labelText: 'Correction',
                hintText: 'e.g., the',
                border: OutlineInputBorder(),
              ),
              textCapitalization: TextCapitalization.none,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () {
              final misspelled = _misspelledController.text.trim();
              final correction = _correctionController.text.trim();
              
              if (misspelled.isNotEmpty && correction.isNotEmpty) {
                context.read<CorrectionService>().addRule(misspelled, correction);
                Navigator.pop(context);
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('Rule added successfully')),
                );
              }
            },
            child: Text('Add'),
          ),
        ],
      ),
    );
  }

  void _showEditRuleDialog(CorrectionRule rule, int index) {
    _misspelledController.text = rule.misspelled;
    _correctionController.text = rule.correction;
    
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Edit Correction Rule'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: _misspelledController,
              decoration: InputDecoration(
                labelText: 'Misspelled word',
                border: OutlineInputBorder(),
              ),
              textCapitalization: TextCapitalization.none,
            ),
            SizedBox(height: 16),
            TextField(
              controller: _correctionController,
              decoration: InputDecoration(
                labelText: 'Correction',
                border: OutlineInputBorder(),
              ),
              textCapitalization: TextCapitalization.none,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () {
              final misspelled = _misspelledController.text.trim();
              final correction = _correctionController.text.trim();
              
              if (misspelled.isNotEmpty && correction.isNotEmpty) {
                context.read<CorrectionService>().addRule(misspelled, correction);
                Navigator.pop(context);
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('Rule updated successfully')),
                );
              }
            },
            child: Text('Update'),
          ),
        ],
      ),
    );
  }

  void _showDeleteConfirmation(int index) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Delete Rule'),
        content: Text('Are you sure you want to delete this correction rule?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () {
              context.read<CorrectionService>().removeRule(index);
              Navigator.pop(context);
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text('Rule deleted')),
              );
            },
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
            child: Text('Delete', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );
  }
}
