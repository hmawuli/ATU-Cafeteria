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
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _fullNameController = TextEditingController();
  
  final _infoController = TextEditingController();
  String _role = 'STUDENT'; // 'STUDENT' or 'VENDOR'

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _fullNameController.dispose();
    _infoController.dispose();
    super.dispose();
  }

  void _submit() async {
    if (!_formKey.currentState!.validate()) return;

    final provider = Provider.of<CafeteriaProvider>(context, listen: false);
    final success = await provider.registerUser(
      email: _emailController.text.trim(),
      password: _passwordController.text,
      role: _role,
      fullName: _fullNameController.text.trim(),
      info: _infoController.text.trim(),
      
    );

    if (success && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text("Registration successful. You can now sign in with your email and password."),
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
        title: const Text('CREATE YOUR ACCOUNT'),
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
                    'ATU CAFETERIA',
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Create a secure account to access the ATU Cafeteria platform.',
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
                            'Account Details',
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
                              labelText: 'Email Address',
                              prefixIcon: Icon(Icons.email_outlined),
                              border: OutlineInputBorder(),
                            ),
                            validator: (val) {
                              final value = val?.trim() ?? '';
                              if (value.isEmpty) return 'Enter your email address';
                              return RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+ const InputDecoration(
                              labelText: 'Email Address',
                              prefixIcon: Icon(Icons.person),
                              border: OutlineInputBorder(),
                            ),
                            validator: (val) =>
                                val == null || val.trim().isEmpty ? 'Enter unique username' : null,
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: _passwordController,
                            obscureText: true,
                            decoration: const InputDecoration(
                              labelText: 'Password (minimum 8 characters)',
                              prefixIcon: Icon(Icons.lock_person),
                              border: OutlineInputBorder(),
                            ),
                            validator: (val) =>
                                val == null || val.length < 8 ? 'Use at least 8 characters' : null,
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
                                  ? 'Department / Programme (optional)'
                                  : 'Vendor Food-Booth Brand Name (e.g., Kofi Rice Joint)',
                              prefixIcon: const Icon(Icons.business_center),
                              border: const OutlineInputBorder(),
                              helperText: 'Student ID is not required for login.',
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
).hasMatch(value)
                                  ? null
                                  : 'Enter a valid email address';
                            },
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: _passwordController,
                            obscureText: true,
                            decoration: const InputDecoration(
                              labelText: 'Password (minimum 8 characters)',
                              prefixIcon: Icon(Icons.lock_person),
                              border: OutlineInputBorder(),
                            ),
                            validator: (val) =>
                                val == null || val.length < 8 ? 'Use at least 8 characters' : null,
                          ),
                          const SizedBox(height: 16),
                          DropdownButtonFormField<String>(
 const InputDecoration(
                              labelText: 'Email Address',
                              prefixIcon: Icon(Icons.person),
                              border: OutlineInputBorder(),
                            ),
                            validator: (val) =>
                                val == null || val.trim().isEmpty ? 'Enter unique username' : null,
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: _passwordController,
                            obscureText: true,
                            decoration: const InputDecoration(
                              labelText: 'Password (minimum 8 characters)',
                              prefixIcon: Icon(Icons.lock_person),
                              border: OutlineInputBorder(),
                            ),
                            validator: (val) =>
                                val == null || val.length < 8 ? 'Use at least 8 characters' : null,
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
                                  ? 'Department / Programme (optional)'
                                  : 'Vendor Food-Booth Brand Name (e.g., Kofi Rice Joint)',
                              prefixIcon: const Icon(Icons.business_center),
                              border: const OutlineInputBorder(),
                              helperText: 'Student ID is not required for login.',
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
