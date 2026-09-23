/// Build-time switch for the standalone phone-only defense APK.
///
/// Enable only with:
/// flutter build apk --release --dart-define=ATU_DEFENSE_MODE=true
class DefenseConfig {
  DefenseConfig._();

  static const bool enabled =
      bool.fromEnvironment('ATU_DEFENSE_MODE', defaultValue: false);
}
