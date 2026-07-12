import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../features/dashboard/dashboard_screen.dart';
import '../../features/devvocab/devvocab_screen.dart';
import '../../features/devvocab/devvocab_lesson_screen.dart';
import '../../features/membyte/membyte_screen.dart';
import '../../features/membyte/membyte_review_screen.dart';
import '../../features/techtalk/techtalk_select_screen.dart';
import '../../features/techtalk/techtalk_screen.dart';
import '../../features/techtalk/techtalk_result_screen.dart';
import '../../features/honorifics/honorifics_screen.dart';
import '../../features/video/video_screen.dart';
import '../../features/pronunciation/pronunciation_list_screen.dart';
import '../../features/pronunciation/pronunciation_screen.dart';
import '../../features/admin/admin_panel_screen.dart';
import '../../features/profile/profile_screen.dart';
import '../../features/main/main_screen.dart';
import '../../features/auth/login_screen.dart';
import '../../features/auth/register_screen.dart';
import '../../features/auth/forgot_password_screen.dart';
import '../../features/onboarding/onboarding_screen.dart';

final GlobalKey<NavigatorState> _rootNavigatorKey = GlobalKey<NavigatorState>();
final GlobalKey<NavigatorState> _shellNavigatorKey = GlobalKey<NavigatorState>();

final GoRouter appRouter = GoRouter(
  navigatorKey: _rootNavigatorKey,
  initialLocation: '/login',
  routes: [
    GoRoute(
      path: '/login',
      pageBuilder: (context, state) => const NoTransitionPage(child: LoginScreen()),
    ),
    GoRoute(
      path: '/register',
      pageBuilder: (context, state) => const NoTransitionPage(child: RegisterScreen()),
    ),
    GoRoute(
      path: '/forgot-password',
      pageBuilder: (context, state) => const NoTransitionPage(child: ForgotPasswordScreen()),
    ),
    GoRoute(
      path: '/onboarding',
      pageBuilder: (context, state) => const NoTransitionPage(child: OnboardingScreen()),
    ),
    GoRoute(
      path: '/devvocab-lesson/:id',
      pageBuilder: (context, state) {
        final id = state.pathParameters['id'];
        return NoTransitionPage(child: DevVocabLessonScreen(lessonId: id));
      },
    ),
    GoRoute(
      path: '/membyte-review/:id',
      pageBuilder: (context, state) {
        final id = state.pathParameters['id'];
        return NoTransitionPage(child: MemByteReviewScreen(deckId: id));
      },
    ),
    GoRoute(
      path: '/techtalk-select',
      pageBuilder: (context, state) => const NoTransitionPage(child: TechTalkSelectScreen()),
    ),
    GoRoute(
      path: '/techtalk-chat',
      pageBuilder: (context, state) {
        final scenario = state.extra as Map<String, dynamic>?;
        return NoTransitionPage(child: TechTalkScreen(scenario: scenario));
      },
    ),
    GoRoute(
      path: '/techtalk-result',
      pageBuilder: (context, state) => const NoTransitionPage(child: TechTalkResultScreen()),
    ),
    GoRoute(
      path: '/honorifics',
      pageBuilder: (context, state) => const NoTransitionPage(child: HonorificsScreen()),
    ),
    GoRoute(
      path: '/video',
      pageBuilder: (context, state) => const NoTransitionPage(child: VideoScreen()),
    ),
    GoRoute(
      path: '/pronunciation-list',
      pageBuilder: (context, state) => const NoTransitionPage(child: PronunciationListScreen()),
    ),
    GoRoute(
      path: '/pronunciation',
      pageBuilder: (context, state) => const NoTransitionPage(child: PronunciationScreen()),
    ),
    GoRoute(
      path: '/admin',
      pageBuilder: (context, state) => const NoTransitionPage(child: AdminPanelScreen()),
    ),
    ShellRoute(
      navigatorKey: _shellNavigatorKey,
      builder: (context, state, child) {
        return MainScreen(child: child);
      },
      routes: [
        GoRoute(
          path: '/dashboard',
          pageBuilder: (context, state) => const NoTransitionPage(child: DashboardScreen()),
        ),
        GoRoute(
          path: '/devvocab',
          pageBuilder: (context, state) => const NoTransitionPage(child: DevVocabScreen()),
        ),
        GoRoute(
          path: '/membyte',
          pageBuilder: (context, state) => const NoTransitionPage(child: MemByteScreen()),
        ),
        GoRoute(
          path: '/profile',
          pageBuilder: (context, state) => const NoTransitionPage(child: ProfileScreen()),
        ),
      ],
    ),
  ],
);
