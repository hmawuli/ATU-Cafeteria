// Keep sqflite out of Flutter Web builds. Mobile/desktop use SQLite; web gets
// an explicit unsupported implementation because defense mode is an APK-only path.
export 'defense_local_api_stub.dart'
    if (dart.library.io) 'defense_local_api_io.dart';
