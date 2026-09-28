/// Helpers for decoding restaurant menu responses from the backend.
///
/// The catalogue endpoints return `{success: true, menu_items: [...]}`. To
/// stay compatible with older deployments we also accept `{data: [...]}` and
/// bare JSON arrays. Keep parsing logic here so every consumer behaves the
/// same and the behaviour is unit-testable.
library;

/// Returns the list of menu item maps/objects from a decoded JSON response,
/// or `null` when the payload has no usable list (malformed or empty non-list
/// shapes return an empty list through the caller).
List<dynamic>? menuItemsFromResponse(dynamic decoded) {
  if (decoded is Map<String, dynamic> && decoded['menu_items'] is List) {
    return decoded['menu_items'] as List<dynamic>;
  }
  if (decoded is Map<String, dynamic> && decoded['data'] is List) {
    return decoded['data'] as List<dynamic>;
  }
  if (decoded is List) {
    return decoded;
  }
  return null;
}