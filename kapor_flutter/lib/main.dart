import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'core/config/app_environment.dart';
import 'core/theme/app_theme.dart';
import 'core/router/app_router.dart';
import 'features/auth/providers/auth_provider.dart';
import 'core/providers/settings_provider.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await AppEnvironment.load();
  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AuthProvider()),
        ChangeNotifierProvider(create: (_) => SettingsProvider()),
      ],
      child: const KaporApp(),
    ),
  );
}

class KaporApp extends StatelessWidget {
  const KaporApp({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<SettingsProvider>(
      builder: (context, settingsProvider, child) {
        return MaterialApp.router(
          title: 'Kapor',
          debugShowCheckedModeBanner: false,
          themeMode: settingsProvider.themeMode,
          theme: AppTheme.lightTheme,
          darkTheme: AppTheme.darkTheme,
          routerConfig: appRouter,
        );
      },
    );
  }
}
