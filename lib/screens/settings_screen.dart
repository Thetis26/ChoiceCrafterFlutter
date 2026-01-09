import 'package:flutter/material.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({
    super.key,
    required this.language,
    required this.themeMode,
    required this.onLanguageChanged,
    required this.onThemeModeChanged,
  });

  final String language;
  final ThemeMode themeMode;
  final ValueChanged<String> onLanguageChanged;
  final ValueChanged<ThemeMode> onThemeModeChanged;

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  bool notificationsEnabled = true;
  bool analyticsSharing = false;
  late String language;
  late ThemeMode themeMode;

  @override
  void initState() {
    super.initState();
    language = widget.language;
    themeMode = widget.themeMode;
  }

  @override
  void didUpdateWidget(covariant SettingsScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.language != widget.language) {
      language = widget.language;
    }
    if (oldWidget.themeMode != widget.themeMode) {
      themeMode = widget.themeMode;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
      ),
      body: SafeArea(
        child: ListView(
          children: [
            SwitchListTile(
              title: const Text('Push notifications'),
              subtitle: const Text('Match the Android notification behavior'),
              value: notificationsEnabled,
              onChanged: (value) => setState(() => notificationsEnabled = value),
            ),
            SwitchListTile(
              title: const Text('Share anonymous analytics'),
              subtitle: const Text('Use the same metrics as the Android build'),
              value: analyticsSharing,
              onChanged: (value) => setState(() => analyticsSharing = value),
            ),
            ListTile(
              title: const Text('Language'),
              subtitle: Text(language),
              trailing: const Icon(Icons.chevron_right),
              onTap: () async {
                final selected = await showDialog<String>(
                  context: context,
                  builder: (context) => SimpleDialog(
                    title: const Text('Choose language'),
                    children: [
                      SimpleDialogOption(
                        onPressed: () => Navigator.of(context).pop('English'),
                        child: const Text('English'),
                      ),
                      SimpleDialogOption(
                        onPressed: () => Navigator.of(context).pop('Romanian'),
                        child: const Text('Romanian'),
                      ),
                    ],
                  ),
                );
                if (selected != null) {
                  setState(() => language = selected);
                  widget.onLanguageChanged(selected);
                }
              },
            ),
            ListTile(
              title: const Text('Theme'),
              subtitle: Text(themeMode == ThemeMode.dark ? 'Dark' : 'Light'),
              trailing: const Icon(Icons.chevron_right),
              onTap: () async {
                final selected = await showDialog<ThemeMode>(
                  context: context,
                  builder: (context) => SimpleDialog(
                    title: const Text('Choose theme'),
                    children: [
                      SimpleDialogOption(
                        onPressed: () =>
                            Navigator.of(context).pop(ThemeMode.light),
                        child: const Text('Light'),
                      ),
                      SimpleDialogOption(
                        onPressed: () =>
                            Navigator.of(context).pop(ThemeMode.dark),
                        child: const Text('Dark'),
                      ),
                    ],
                  ),
                );
                if (selected != null) {
                  setState(() => themeMode = selected);
                  widget.onThemeModeChanged(selected);
                }
              },
            ),
            const Divider(),
            ListTile(
              leading: const Icon(Icons.logout),
              title: const Text('Logout'),
              subtitle: const Text('Mimics the Android navigation drawer action'),
              onTap: () => ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Logout tapped (demo only)')),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
