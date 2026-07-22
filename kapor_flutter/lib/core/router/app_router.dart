import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../features/dashboard/dashboard_screen.dart';
import '../../features/devvocab/devvocab_screen.dart';
import '../../features/devvocab/devvocab_lesson_screen.dart';
import '../../features/devvocab/devvocab_lesson_detail_screen.dart';
import '../../features/devvocab/data/devvocab_service.dart';
import '../../features/devvocab/flashcard_study_screen.dart';
import '../../features/devvocab/flashcard_summary_screen.dart';
import '../../features/membyte/membyte_screen.dart';
import '../../features/membyte/membyte_review_screen.dart';
import '../../features/techtalk/techtalk_select_screen.dart';
import '../../features/techtalk/techtalk_screen.dart';
import '../../features/techtalk/techtalk_result_screen.dart';
import '../../features/techtalk/data/techtalk_service.dart';
import '../../features/honorifics/honorifics_screen.dart';
import '../../features/video/video_screen.dart';
import '../../features/pronunciation/pronunciation_list_screen.dart';
import '../../features/pronunciation/pronunciation_screen.dart';
import '../../features/pronunciation/data/pronunciation_service.dart';
import '../../features/admin/admin_panel_screen.dart';
import '../../features/profile/profile_screen.dart';
import '../../features/main/main_screen.dart';
import '../../features/auth/login_screen.dart';
import '../../features/auth/register_screen.dart';
import '../../features/auth/forgot_password_screen.dart';
import '../../features/onboarding/onboarding_screen.dart';

final GlobalKey<NavigatorState> _rootNavigatorKey = GlobalKey<NavigatorState>();
final GlobalKey<NavigatorState> _shellNavigatorKey =
    GlobalKey<NavigatorState>();

final GoRouter appRouter = GoRouter(
  navigatorKey: _rootNavigatorKey,
  initialLocation: '/login',
  routes: [
    GoRoute(
      path: '/login',
      pageBuilder: (context, state) =>
          const NoTransitionPage(child: LoginScreen()),
    ),
    GoRoute(
      path: '/register',
      pageBuilder: (context, state) =>
          const NoTransitionPage(child: RegisterScreen()),
    ),
    GoRoute(
      path: '/forgot-password',
      pageBuilder: (context, state) =>
          const NoTransitionPage(child: ForgotPasswordScreen()),
    ),
    GoRoute(
      path: '/onboarding',
      pageBuilder: (context, state) =>
          const NoTransitionPage(child: OnboardingScreen()),
    ),
    GoRoute(
      path: '/devvocab-topic/:id',
      pageBuilder: (context, state) {
        final id = state.pathParameters['id'];
        final topic = state.extra is DevVocabTopic
            ? state.extra as DevVocabTopic
            : null;
        return NoTransitionPage(
          child: DevVocabLessonScreen(topicId: id ?? '', topic: topic),
        );
      },
    ),
    GoRoute(
      path: '/devvocab-lesson/:id',
      pageBuilder: (context, state) {
        final lesson = state.extra is DevVocabLesson
            ? state.extra as DevVocabLesson
            : null;
        return CustomTransitionPage<void>(
          key: state.pageKey,
          transitionDuration: const Duration(milliseconds: 260),
          reverseTransitionDuration: const Duration(milliseconds: 220),
          child: DevVocabLessonDetailScreen(
            lessonId: state.pathParameters['id'] ?? '',
            initialLesson: lesson,
          ),
          transitionsBuilder: (context, animation, secondaryAnimation, child) {
            final curvedAnimation = CurvedAnimation(
              parent: animation,
              curve: Curves.easeOutCubic,
              reverseCurve: Curves.easeInCubic,
            );
            return FadeTransition(
              opacity: curvedAnimation,
              child: SlideTransition(
                position: Tween<Offset>(
                  begin: const Offset(0.035, 0),
                  end: Offset.zero,
                ).animate(curvedAnimation),
                child: ScaleTransition(
                  scale: Tween<double>(
                    begin: 0.985,
                    end: 1,
                  ).animate(curvedAnimation),
                  child: child,
                ),
              ),
            );
          },
        );
      },
    ),
    GoRoute(
      path: '/devvocab-lesson/:id/flashcards',
      pageBuilder: (context, state) {
        final lesson = state.extra is DevVocabLesson
            ? state.extra as DevVocabLesson
            : null;
        return NoTransitionPage(
          child: FlashcardStudyScreen(
            lessonId: state.pathParameters['id'] ?? '',
            initialLesson: lesson,
          ),
        );
      },
    ),
    GoRoute(
      path: '/devvocab-lesson/:id/flashcards/summary',
      pageBuilder: (context, state) {
        final args = state.extra;
        if (args is! FlashcardSummaryArgs) {
          return const NoTransitionPage(
            child: Scaffold(body: SizedBox.shrink()),
          );
        }
        return NoTransitionPage(child: FlashcardSummaryScreen(args: args));
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
      pageBuilder: (context, state) =>
          const NoTransitionPage(child: TechTalkSelectScreen()),
    ),
    GoRoute(
      path: '/techtalk-chat',
      pageBuilder: (context, state) {
        final scenario = state.extra as TechTalkScenario?;
        if (scenario == null) {
          return const NoTransitionPage(child: TechTalkSelectScreen());
        }
        return NoTransitionPage(child: TechTalkScreen(scenario: scenario));
      },
    ),
    GoRoute(
      path: '/techtalk-result',
      pageBuilder: (context, state) => NoTransitionPage(
        child: TechTalkResultScreen(session: state.extra as RoleplaySession?),
      ),
    ),
    GoRoute(
      path: '/honorifics',
      pageBuilder: (context, state) =>
          const NoTransitionPage(child: HonorificsScreen()),
    ),
    GoRoute(
      path: '/video',
      pageBuilder: (context, state) =>
          const NoTransitionPage(child: VideoScreen()),
    ),
    GoRoute(
      path: '/pronunciation-list',
      pageBuilder: (context, state) =>
          const NoTransitionPage(child: PronunciationListScreen()),
    ),
    GoRoute(
      path: '/pronunciation',
      pageBuilder: (context, state) {
        final exercise = state.extra as PronunciationExercise?;
        if (exercise == null) {
          return const NoTransitionPage(child: PronunciationListScreen());
        }
        return NoTransitionPage(child: PronunciationScreen(exercise: exercise));
      },
    ),
    GoRoute(
      path: '/admin',
      pageBuilder: (context, state) =>
          const NoTransitionPage(child: AdminPanelScreen()),
    ),
    ShellRoute(
      navigatorKey: _shellNavigatorKey,
      builder: (context, state, child) {
        return MainScreen(child: child);
      },
      routes: [
        GoRoute(
          path: '/dashboard',
          pageBuilder: (context, state) =>
              const NoTransitionPage(child: DashboardScreen()),
        ),
        GoRoute(
          path: '/devvocab',
          pageBuilder: (context, state) =>
              const NoTransitionPage(child: DevVocabScreen()),
        ),
        GoRoute(
          path: '/membyte',
          pageBuilder: (context, state) =>
              const NoTransitionPage(child: MemByteScreen()),
        ),
        GoRoute(
          path: '/profile',
          pageBuilder: (context, state) =>
              const NoTransitionPage(child: ProfileScreen()),
        ),
      ],
    ),
  ],
);
