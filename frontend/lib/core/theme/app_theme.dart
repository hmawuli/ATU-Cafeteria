import 'package:flutter/material.dart';

/// Shared ATU Cafeteria visual system.
///
/// The theme keeps the application lightweight while giving customer,
/// vendor and admin surfaces one consistent production visual language.
class AppTheme {
  static const Color primary = Color(0xFF063B82);
  static const Color primaryDark = Color(0xFF052B63);
  static const Color primaryLight = Color(0xFFEAF2FC);
  static const Color accent = Color(0xFFFFC400);
  static const Color success = Color(0xFF20B95A);
  static const Color warning = Color(0xFFE39A00);
  static const Color danger = Color(0xFFD64545);
  static const Color background = Color(0xFFF4F8FD);
  static const Color textDark = Color(0xFF0A2E68);
  static const Color textMuted = Color(0xFF5F7188);
  static const Color border = Color(0xFFD7E3F0);

  static ThemeData light() {
    final scheme = ColorScheme.fromSeed(
      seedColor: primary,
      primary: primary,
      secondary: accent,
      brightness: Brightness.light,
      surface: Colors.white,
    ).copyWith(
      onPrimary: Colors.white,
      onSecondary: primaryDark,
      surface: Colors.white,
      onSurface: textDark,
      error: danger,
      onError: Colors.white,
    );

    const textTheme = TextTheme(
      displaySmall: TextStyle(fontSize: 30, fontWeight: FontWeight.w800, height: 1.15),
      headlineSmall: TextStyle(fontSize: 24, fontWeight: FontWeight.w800, height: 1.2),
      titleLarge: TextStyle(fontSize: 20, fontWeight: FontWeight.w800, height: 1.25),
      titleMedium: TextStyle(fontSize: 16, fontWeight: FontWeight.w700, height: 1.3),
      titleSmall: TextStyle(fontSize: 14, fontWeight: FontWeight.w700, height: 1.3),
      bodyLarge: TextStyle(fontSize: 16, fontWeight: FontWeight.w400, height: 1.4),
      bodyMedium: TextStyle(fontSize: 14, fontWeight: FontWeight.w400, height: 1.4),
      bodySmall: TextStyle(fontSize: 12, fontWeight: FontWeight.w400, height: 1.35),
      labelLarge: TextStyle(fontSize: 14, fontWeight: FontWeight.w700, height: 1.2),
      labelMedium: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, height: 1.2),
      labelSmall: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, height: 1.2),
    );

    const rounded = RoundedRectangleBorder(
      borderRadius: BorderRadius.all(Radius.circular(16)),
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: background,
      fontFamily: 'Roboto',
      textTheme: textTheme,
      visualDensity: VisualDensity.adaptivePlatformDensity,
      splashFactory: InkRipple.splashFactory,
      materialTapTargetSize: MaterialTapTargetSize.padded,
      appBarTheme: const AppBarTheme(
        backgroundColor: primary,
        foregroundColor: Colors.white,
        centerTitle: false,
        elevation: 0,
        scrolledUnderElevation: 0,
        toolbarHeight: 64,
        titleTextStyle: TextStyle(
          color: Colors.white,
          fontSize: 20,
          fontWeight: FontWeight.w800,
          letterSpacing: .1,
        ),
        iconTheme: IconThemeData(color: Colors.white),
      ),
      snackBarTheme: const SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        backgroundColor: primaryDark,
        contentTextStyle: TextStyle(
          color: Colors.white,
          fontSize: 14,
          fontWeight: FontWeight.w600,
        ),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(12)),
        ),
        insetPadding: EdgeInsets.fromLTRB(16, 0, 16, 16),
      ),
      dividerTheme: const DividerThemeData(
        space: 1,
        thickness: 1,
        color: Color(0xFFE2EAF3),
      ),
      cardTheme: const CardThemeData(
        margin: EdgeInsets.zero,
        elevation: 1,
        color: Colors.white,
        surfaceTintColor: Colors.transparent,
        shadowColor: Color(0x18062E68),
        shape: rounded,
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
        elevation: 6,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        titleTextStyle: const TextStyle(
          color: textDark,
          fontSize: 20,
          fontWeight: FontWeight.w800,
        ),
        contentTextStyle: const TextStyle(
          color: textMuted,
          fontSize: 14,
          height: 1.4,
        ),
      ),
      bottomSheetTheme: const BottomSheetThemeData(
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
        elevation: 8,
        showDragHandle: true,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
      ),
      listTileTheme: const ListTileThemeData(
        contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 4),
        minLeadingWidth: 28,
        iconColor: primary,
        textColor: textDark,
        titleTextStyle: TextStyle(
          color: textDark,
          fontSize: 15,
          fontWeight: FontWeight.w700,
        ),
        subtitleTextStyle: TextStyle(
          color: textMuted,
          fontSize: 13,
          height: 1.3,
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: Colors.white,
        labelStyle: const TextStyle(
          color: textMuted,
          fontSize: 14,
          fontWeight: FontWeight.w500,
        ),
        hintStyle: const TextStyle(
          color: Color(0xFF7A8CA2),
          fontSize: 14,
        ),
        prefixIconColor: primary,
        suffixIconColor: textMuted,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: border),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: primary, width: 2),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: danger),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: danger, width: 2),
        ),
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: primary,
          foregroundColor: Colors.white,
          minimumSize: const Size(0, 48),
          padding: const EdgeInsets.symmetric(horizontal: 18),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          textStyle: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: primary,
          foregroundColor: Colors.white,
          minimumSize: const Size(0, 48),
          padding: const EdgeInsets.symmetric(horizontal: 18),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          elevation: 1,
          textStyle: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: primary,
          minimumSize: const Size(0, 46),
          padding: const EdgeInsets.symmetric(horizontal: 16),
          side: const BorderSide(color: Color(0xFFB8CDE3), width: 1.1),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          textStyle: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: primary,
          minimumSize: const Size(0, 42),
          padding: const EdgeInsets.symmetric(horizontal: 12),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          textStyle: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700),
        ),
      ),
      navigationBarTheme: NavigationBarThemeData(
        height: 72,
        backgroundColor: primary,
        surfaceTintColor: Colors.transparent,
        elevation: 8,
        indicatorColor: accent,
        labelTextStyle: WidgetStateProperty.resolveWith((states) {
          final selected = states.contains(WidgetState.selected);
          return TextStyle(
            color: selected ? primaryDark : Colors.white,
            fontSize: 11,
            fontWeight: selected ? FontWeight.w800 : FontWeight.w600,
          );
        }),
        iconTheme: WidgetStateProperty.resolveWith((states) {
          final selected = states.contains(WidgetState.selected);
          return IconThemeData(
            color: selected ? primaryDark : Colors.white,
            size: 23,
          );
        }),
      ),
      navigationRailTheme: const NavigationRailThemeData(
        backgroundColor: Colors.white,
        selectedIconTheme: IconThemeData(color: primaryDark),
        unselectedIconTheme: IconThemeData(color: textMuted),
        selectedLabelTextStyle: TextStyle(
          color: primaryDark,
          fontWeight: FontWeight.w800,
        ),
        unselectedLabelTextStyle: TextStyle(
          color: textMuted,
          fontWeight: FontWeight.w600,
        ),
      ),
      floatingActionButtonTheme: const FloatingActionButtonThemeData(
        backgroundColor: accent,
        foregroundColor: primaryDark,
        elevation: 3,
      ),
      chipTheme: ChipThemeData(
        backgroundColor: Colors.white,
        selectedColor: accent,
        side: const BorderSide(color: border),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        labelStyle: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700),
      ),
      progressIndicatorTheme: const ProgressIndicatorThemeData(
        color: primary,
        linearTrackColor: border,
        circularTrackColor: border,
      ),
      tooltipTheme: TooltipThemeData(
        decoration: BoxDecoration(
          color: primaryDark,
          borderRadius: BorderRadius.circular(8),
        ),
        textStyle: const TextStyle(color: Colors.white, fontSize: 12),
      ),
      popupMenuTheme: PopupMenuThemeData(
        color: Colors.white,
        surfaceTintColor: Colors.transparent,
        elevation: 5,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
      tabBarTheme: const TabBarThemeData(
        labelColor: primary,
        unselectedLabelColor: textMuted,
        labelStyle: TextStyle(fontWeight: FontWeight.w800),
        unselectedLabelStyle: TextStyle(fontWeight: FontWeight.w600),
        indicatorColor: accent,
        dividerColor: border,
      ),
    );
  }

  static ThemeData dark() {
    final scheme = ColorScheme.fromSeed(
      seedColor: primary,
      primary: const Color(0xFF74A8FF),
      secondary: accent,
      brightness: Brightness.dark,
    );
    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: const Color(0xFF071A31),
      fontFamily: 'Roboto',
      textTheme: const TextTheme(
        headlineSmall: TextStyle(fontSize: 24, fontWeight: FontWeight.w800),
        titleLarge: TextStyle(fontSize: 20, fontWeight: FontWeight.w800),
        titleMedium: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
        bodyLarge: TextStyle(fontSize: 16, height: 1.4),
        bodyMedium: TextStyle(fontSize: 14, height: 1.4),
        bodySmall: TextStyle(fontSize: 12, height: 1.35),
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: primaryDark,
        foregroundColor: Colors.white,
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
      cardTheme: const CardThemeData(
        color: Color(0xFF102946),
        surfaceTintColor: Colors.transparent,
        elevation: 2,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(16)),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: const Color(0xFF102946),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: Color(0xFF2B496B)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: Color(0xFF2B496B)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: Color(0xFF74A8FF), width: 2),
        ),
      ),
      navigationBarTheme: const NavigationBarThemeData(
        backgroundColor: primaryDark,
        indicatorColor: accent,
      ),
    );
  }

  const AppTheme._();

  // -------------------------------------------------------------------
  // Extended palette — exact legacy values migrated into tokens so the
  // presentation layer uses AppTheme everywhere without visual change.
  // -------------------------------------------------------------------
  static const Color grey900 = Color(0x12000000);
  static const Color blue700 = Color(0x8C052B63);
  static const Color blue700A = Color(0xEB052B63);
  static const Color blue700B = Color(0xFF062B66);
  static const Color blue700C = Color(0xFF073B82);
  static const Color blue600 = Color(0xFF0A4C9C);
  static const Color blue800 = Color(0xFF0B1F3A);
  static const Color blue600A = Color(0xFF0D55B5);
  static const Color blue700D = Color(0xFF123B5D);
  static const Color blue500 = Color(0xFF1565C0);
  static const Color green600 = Color(0xFF178A49);
  static const Color green600A = Color(0xFF20A45A);
  static const Color indigo600 = Color(0xFF283593);
  static const Color green600B = Color(0xFF2E7D32);
  static const Color green600C = Color(0xFF388E3C);
  static const Color blue400 = Color(0xFF4B8DE0);
  static const Color blue500A = Color(0xFF4F6783);
  static const Color blue500B = Color(0xFF61738A);
  static const Color blue500C = Color(0xFF65788D);
  static const Color blue300 = Color(0xFF8CA0B7);
  static const Color orange600 = Color(0xFF9A6A00);
  static const Color blue200 = Color(0xFF9FB7D3);
  static const Color red500 = Color(0xFFB3261E);
  static const Color blue200A = Color(0xFFB8C9DC);
  static const Color blue200B = Color(0xFFB8CDE3);
  static const Color red500A = Color(0xFFC62828);
  static const Color red400 = Color(0xFFD32F2F);
  static const Color blue100 = Color(0xFFD5E0ED);
  static const Color blue100A = Color(0xFFD6E1EF);
  static const Color blue100B = Color(0xFFD8E3F0);
  static const Color orange400 = Color(0xFFE5A93C);
  static const Color orange400A = Color(0xFFE67E22);
  static const Color blue50 = Color(0xFFE7F4FF);
  static const Color orange400B = Color(0xFFE8751A);
  static const Color blue100C = Color(0xFFE8EEF5);
  static const Color blue100D = Color(0xFFE8EEF6);
  static const Color green100 = Color(0xFFE8F8EE);
  static const Color blue50A = Color(0xFFF0F4F8);
  static const Color orange500 = Color(0xFFF57C00);
  static const Color grey50 = Color(0xFFF5F8FC);
  static const Color red200 = Color(0xFFFF7777);
  static const Color orange500A = Color(0xFFFFA000);
  static const Color red50 = Color(0xFFFFE9E9);
  static const Color amber100 = Color(0xFFFFF6D6);
}
