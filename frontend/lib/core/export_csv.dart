import 'dart:io';

import 'package:path_provider/path_provider.dart';

/// Saves CSV text to the device Downloads (or documents) folder and returns
/// the file path, so supervisors can keep exports for records.
Future<String> saveCsvFile(String filename, String contents) async {
  final downloads = await getDownloadsDirectory();
  final dir = downloads ?? await getApplicationDocumentsDirectory();
  final file = File('${dir.path}/$filename');
  await file.writeAsString(contents);
  return file.path;
}