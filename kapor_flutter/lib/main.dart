import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'core/theme/app_theme.dart';
import 'core/router/app_router.dart';
import 'features/auth/providers/auth_provider.dart';

void main() {
  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AuthProvider()),
        Provider<bool>.value(value: true),
      ],
      child: const KaporApp(),
    ),
  );
}

class KaporApp extends StatelessWidget {
  const KaporApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'Kapor',
      debugShowCheckedModeBanner: false,
      themeMode: ThemeMode.dark, // Force dark mode as per aesthetic stance
      darkTheme: AppTheme.darkTheme,
      routerConfig: appRouter,
    );
  }
}
