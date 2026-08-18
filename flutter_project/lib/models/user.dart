class User {
  final int id;
  final String username;
  final String fullName;
  final String role; // 'STUDENT', 'VENDOR', 'ADMIN'
  final String? studentStaffId;
  final String? telephone;
  final String? email;
  final String? department;
  final String? programOfStudy;
  final double balance;
  final bool isOpen;

  User({
    required this.id,
    required this.username,
    required this.fullName,
    required this.role,
    this.studentStaffId,
    this.telephone,
    this.email,
    this.department,
    this.programOfStudy,
    this.balance = 0.0,
    this.isOpen = true,
  });

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] ?? 0,
      username: json['username'] ?? '',
      fullName: json['fullName'] ?? json['full_name'] ?? '',
      role: (json['role'] ?? 'STUDENT').toString().toUpperCase(),
      studentStaffId: json['student_staff_id'] ?? json['studentStaffId'],
      telephone: json['telephone'] ?? json['phone_number'],
      email: json['email'],
      department: json['department'],
      programOfStudy: json['program_of_study'] ?? json['programOfStudy'],
      balance: (json['balance'] as num?)?.toDouble() ?? 0.0,
      isOpen: json['is_open'] == 1 || json['is_open'] == true || json['isOpen'] == true,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'username': username,
      'fullName': fullName,
      'role': role,
      'student_staff_id': studentStaffId,
      'telephone': telephone,
      'email': email,
      'department': department,
      'program_of_study': programOfStudy,
      'balance': balance,
      'is_open': isOpen,
    };
  }
}
