class FoodItem {
  final int id;
  final int vendorId;
  final String name;
  final double price;
  final String category;
  final String imageUrl;
  final String description;
  final bool isAvailable;
  final int calories;
  final String allergens;

  FoodItem({
    required this.id,
    required this.vendorId,
    required this.name,
    required this.price,
    required this.category,
    required this.imageUrl,
    required this.description,
    this.isAvailable = true,
    this.calories = 250,
    this.allergens = 'None',
  });

  factory FoodItem.fromJson(Map<String, dynamic> json) {
    return FoodItem(
      id: json['id'] ?? 0,
      vendorId: json['vendor_id'] ?? json['vendorId'] ?? 0,
      name: json['name'] ?? '',
      price: (json['price'] as num?)?.toDouble() ?? 0.0,
      category: json['category'] ?? 'General',
      imageUrl: json['image_url'] ?? json['imageUrl'] ?? '',
      description: json['description'] ?? '',
      isAvailable: json['is_available'] == 1 || json['is_available'] == true || json['isAvailable'] == true,
      calories: json['calories'] ?? 250,
      allergens: json['allergens'] ?? 'None',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'vendor_id': vendorId,
      'name': name,
      'price': price,
      'category': category,
      'image_url': imageUrl,
      'description': description,
      'is_available': isAvailable,
      'calories': calories,
      'allergens': allergens,
    };
  }
}
