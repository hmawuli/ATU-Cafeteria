import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:atu_cafeteria/presentation/providers/cafeteria_provider.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _usernameController = TextEditingController();
  final _pinController = TextEditingController();
  final _fullNameController = TextEditingController();
  final _emailController = TextEditingController();
  final _infoController = TextEditingController();
  String _role = 'STUDENT'; // 'STUDENT' or 'VENDOR'

  @override
  void dispose() {
    _usernameController.dispose();
    _pinController.dispose();
    _fullNameController.dispose();
    _emailController.dispose();
    _infoController.dispose();
    super.dispose();
  }

  void _submit() async {
    if (!_formKey.currentState!.validate()) return;

    final provider = Provider.of<CafeteriaProvider>(context, listen: false);
    final success = await provider.registerUser(
      username: _usernameController.text.trim(),
      pinCode: _pinController.text.trim(),
      role: _role,
      fullName: _fullNameController.text.trim(),
      info: _infoController.text.trim(),
      email: _emailController.text.trim().isEmpty ? null : _emailController.text.trim(),
    );

    if (success && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text("Registration Successful! Please authenticate with your PIN."),
          backgroundColor: Theme.of(context).colorScheme.primary,
        ),
      );
      Navigator.pushReplacementNamed(context, '/login');
    } else if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(provider.loginError ?? "Registration failed"),
          backgroundColor: Theme.of(context).colorScheme.error,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<CafeteriaProvider>(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('CREATE PORTAL IDENTITY'),
      ),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 500),
            child: Form(
              key: _formKey,
              child: Column(
                children: [
                  Text(
                    'ATU CAFETERIA ARCHITECT',
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Establish credentials for digital food procurement and performance audits.',
                    textAlign: TextAlign.center,
                    style: TextStyle(color: Colors.grey[600], fontSize: 13),
                  ),
                  const SizedBox(height: 24),
                  Card(
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                    color: Colors.white,
                    child: Padding(
                      padding: const EdgeInsets.all(20.0),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'User Profile Details',
                            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                          ),
                          const SizedBox(height: 16),
                          TextFormField(
                            controller: _fullNameController,
                            decoration: const InputDecoration(
                              labelText: 'Full Name (e.g. Daniel Mensah)',
                              prefixIcon: Icon(Icons.badge),
                              border: OutlineInputBorder(),
                            ),
                            validator: (val) =>
                                val == null || val.trim().isEmpty ? 'Enter full legal name' : null,
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: _emailController,
                            keyboardType: TextInputType.emailAddress,
                            decoration: const InputDecoration(
                              labelText: 'Email Address (optional)',
                              prefixIcon: Icon(Icons.email_outlined),
                              border: OutlineInputBorder(),
                            ),
                            validator: (val) {
                              final value = val?.trim() ?? '';
                              if (value.isEmpty) return null;
                              return RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(value) ? null : 'Enter a valid email address';
                            },
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: _usernameController,
                            decoration: const InputDecoration(
                              labelText: 'Username (Unique)',
                              prefixIcon: Icon(Icons.person),
                              border: OutlineInputBorder(),
                            ),
                            validator: (val) =>
                                val == null || val.trim().isEmpty ? 'Enter unique username' : null,
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: _pinController,
                            obscureText: true,
                            keyboardType: TextInputType.number,
                            decoration: const InputDecoration(
                              labelText: 'Security PIN (4+ digits)',
                              prefixIcon: Icon(Icons.lock_person),
                              border: OutlineInputBorder(),
                            ),
                            validator: (val) =>
                                val == null || val.length < 4 ? 'Enter 4+ digits security numeric PIN' : null,
                          ),
                          const SizedBox(height: 16),
                          DropdownButtonFormField<String>(
                            initialValue: _role,
                            decoration: const InputDecoration(
                              labelText: 'Registry Classification',
                              border: OutlineInputBorder(),
                              prefixIcon: Icon(Icons.people_alt),
                            ),
                            items: const [
                              DropdownMenuItem(value: 'STUDENT', child: Text("Student / Officer")),
                              DropdownMenuItem(value: 'VENDOR', child: Text("Food Court Vendor")),
                            ],
                            onChanged: (val) {
                              if (val != null) setState(() => _role = val);
                            },
                          ),
                          const SizedBox(height: 16),
                          TextFormField(
                            controller: _infoController,
                            decoration: InputDecoration(
                              labelText: _role == 'STUDENT'
                                  ? 'Student Registration ID (e.g., ATU-2024-D45)'
                                  : 'Vendor Food-Booth Brand Name (e.g., Kofi Rice Joint)',
                              prefixIcon: const Icon(Icons.business_center),
                              border: const OutlineInputBorder(),
                              helperText: 'Leave empty for automated registry generation.',
                            ),
                          ),
                          const SizedBox(height: 20),
                          provider.isLoading
                              ? const Center(child: CircularProgressIndicator())
                              : ElevatedButton(
                                  onPressed: _submit,
                                  child: const Text('COMMIT NEW USER REGISTRY', style: TextStyle(fontWeight: FontWeight.bold)),
                                ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextButton(
                    onPressed: () => Navigator.pushReplacementNamed(context, '/login'),
                    child: Text(
                      "Already have an account? Return to Login",
                      style: TextStyle(
                        color: Theme.of(context).colorScheme.primary,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
