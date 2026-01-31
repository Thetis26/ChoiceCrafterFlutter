import 'package:flutter/widgets.dart';

class AppLocalizations {
  AppLocalizations(this.locale);

  final Locale locale;

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  static AppLocalizations of(BuildContext context) {
    final localizations = Localizations.of<AppLocalizations>(context, AppLocalizations);
    if (localizations == null) {
      throw StateError('AppLocalizations not found in widget tree.');
    }
    return localizations;
  }

  bool get _isRomanian => locale.languageCode == 'ro';

  String get settings => _isRomanian ? 'Setări' : 'Settings';
  String get signOut => _isRomanian ? 'Deconectare' : 'Sign out';
  String get messages => _isRomanian ? 'Mesaje' : 'Messages';
  String get inbox => _isRomanian ? 'Inbox' : 'Inbox';
  String get feedback => _isRomanian ? 'Feedback' : 'Feedback';
  String get home => _isRomanian ? 'Acasă' : 'Home';
  String get peersActivity =>
      _isRomanian ? 'Activitatea colegilor' : "Peers' activity";
  String get news => _isRomanian ? 'Noutăți' : 'News';
  String get personalStatistics =>
      _isRomanian ? 'Statistici personale' : 'Personal statistics';
  String get pushNotifications =>
      _isRomanian ? 'Notificări push' : 'Push notifications';
  String get matchAndroidNotifications => _isRomanian
      ? 'Potrivește comportamentul notificărilor Android'
      : 'Match the Android notification behavior';
  String get shareAnonymousAnalytics => _isRomanian
      ? 'Partajează analize anonime'
      : 'Share anonymous analytics';
  String get matchAndroidMetrics => _isRomanian
      ? 'Folosește aceleași metrici ca versiunea Android'
      : 'Use the same metrics as the Android build';
  String get language => _isRomanian ? 'Limbă' : 'Language';
  String get chooseLanguage => _isRomanian ? 'Alege limba' : 'Choose language';
  String get theme => _isRomanian ? 'Temă' : 'Theme';
  String get chooseTheme => _isRomanian ? 'Alege tema' : 'Choose theme';
  String get light => _isRomanian ? 'Deschisă' : 'Light';
  String get dark => _isRomanian ? 'Întunecată' : 'Dark';
  String get logout => _isRomanian ? 'Deconectare' : 'Logout';
  String get logoutSubtitle => _isRomanian
      ? 'Imită acțiunea din meniul Android'
      : 'Mimics the Android navigation drawer action';
  String get logoutSnackBar =>
      _isRomanian ? 'Deconectare apăsată (doar demo)' : 'Logout tapped (demo only)';

  String languageName(Locale locale) {
    switch (locale.languageCode) {
      case 'ro':
        return _isRomanian ? 'Română' : 'Romanian';
      case 'en':
      default:
        return _isRomanian ? 'Engleză' : 'English';
    }
  }
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) => ['en', 'ro'].contains(locale.languageCode);

  @override
  Future<AppLocalizations> load(Locale locale) async {
    return AppLocalizations(locale);
  }

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}
