import 'package:flutter/material.dart';
import 'mobile_student_screen.dart';

/// Restaurant-facing customer home.
///
/// The implementation is shared with the existing mobile experience so the
/// customer-domain migration does not duplicate UI or break current flows.
/// The legacy class remains available for backwards compatibility.
class CustomerHomeScreen extends MobileStudentScreen {
  const CustomerHomeScreen({super.key});
}
