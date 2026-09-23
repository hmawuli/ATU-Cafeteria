import 'package:flutter/material.dart';
import 'mobile_student_screen.dart';

/// Primary restaurant customer experience.
///
/// The underlying implementation remains shared with the mature mobile
/// ordering flow, while the public application now presents the domain as
/// Customer rather than Student. This keeps existing accounts and data
/// compatible during the migration.
class CustomerHomeScreen extends MobileStudentScreen {
  const CustomerHomeScreen({super.key});
}
