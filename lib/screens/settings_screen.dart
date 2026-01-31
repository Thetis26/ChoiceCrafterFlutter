import 'package:flutter/material.dart';

import '../localization/app_localizations.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({
    super.key,
    required this.locale,
    required this.themeMode,
    required this.onLanguageChanged,
    required this.onThemeModeChanged,
  });

  final Locale locale;
  final ThemeMode themeMode;
  final ValueChanged<Locale> onLanguageChanged;
  final ValueChanged<ThemeMode> onThemeModeChanged;

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  bool notificationsEnabled = true;
  bool analyticsSharing = false;
  late Locale locale;
  late ThemeMode themeMode;

  @override
  void initState() {
    super.initState();
    locale = widget.locale;
    themeMode = widget.themeMode;
  }

  @override
  void didUpdateWidget(covariant SettingsScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.locale != widget.locale) {
      locale = widget.locale;
    }
    if (oldWidget.themeMode != widget.themeMode) {
      themeMode = widget.themeMode;
    }
  }

  @override
  Widget build(BuildContext context) {
    final localizations = AppLocalizations.of(context);
    return Scaffold(
      appBar: AppBar(
        title: Text(localizations.settings),
      ),
      body: SafeArea(
        child: ListView(
          children: [
            SwitchListTile(
              title: Text(localizations.pushNotifications),
              subtitle: Text(localizations.matchAndroidNotifications),
              value: notificationsEnabled,
              onChanged: (value) => setState(() => notificationsEnabled = value),
            ),
            SwitchListTile(
              title: Text(localizations.shareAnonymousAnalytics),
              subtitle: Text(localizations.matchAndroidMetrics),
              value: analyticsSharing,
              onChanged: (value) => setState(() => analyticsSharing = value),
            ),
            ListTile(
              title: Text(localizations.language),
              subtitle: Text(localizations.languageName(locale)),
              trailing: const Icon(Icons.chevron_right),
              onTap: () async {
                final selected = await showDialog<Locale>(
                  context: context,
                  builder: (context) => SimpleDialog(
                    title: Text(localizations.chooseLanguage),
                    children: [
                      SimpleDialogOption(
                        onPressed: () =>
                            Navigator.of(context).pop(const Locale('en')),
                        child: Text(localizations.languageName(const Locale('en'))),
                      ),
                      SimpleDialogOption(
                        onPressed: () =>
                            Navigator.of(context).pop(const Locale('ro')),
                        child: Text(localizations.languageName(const Locale('ro'))),
                      ),
                    ],
                  ),
                );
                if (selected != null) {
                  setState(() => locale = selected);
                  widget.onLanguageChanged(selected);
                }
              },
            ),
            ListTile(
              title: Text(localizations.theme),
              subtitle: Text(
                themeMode == ThemeMode.dark
                    ? localizations.dark
                    : localizations.light,
              ),
              trailing: const Icon(Icons.chevron_right),
              onTap: () async {
                final selected = await showDialog<ThemeMode>(
                  context: context,
                  builder: (context) => SimpleDialog(
                    title: Text(localizations.chooseTheme),
                    children: [
                      SimpleDialogOption(
                        onPressed: () =>
                            Navigator.of(context).pop(ThemeMode.light),
                        child: Text(localizations.light),
                      ),
                      SimpleDialogOption(
                        onPressed: () =>
                            Navigator.of(context).pop(ThemeMode.dark),
                        child: Text(localizations.dark),
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
              title: Text(localizations.logout),
              subtitle: Text(localizations.logoutSubtitle),
              onTap: () => ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text(localizations.logoutSnackBar)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
