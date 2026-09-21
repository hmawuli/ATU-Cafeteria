/// Configuration for the phone-only project-defense build.
///
/// Build the standalone offline APK with:
///   flutter build apk --release --dart-define=ATU_DEFENSE_MODE=true
///
/// The normal production application remains unchanged when the flag is false.
class DefenseConfig {
  DefenseConfig._();

  static const bool enabled =
      bool.fromEnvironment('ATU_DEFENSE_MODE', defaultValue: false);
}
