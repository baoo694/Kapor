import 'package:google_sign_in/google_sign_in.dart';

import '../../../core/config/app_environment.dart';

/// Obtains a Google OpenID Connect ID token for the Kapor API.
///
/// The backend, rather than the app, verifies the token and creates the Kapor
/// session. No Google client secret is ever bundled with the application.
class GoogleSignInService {
  GoogleSignInService({GoogleSignIn? signIn})
      : _signIn = signIn ?? GoogleSignIn.instance;

  final GoogleSignIn _signIn;
  Future<void>? _initialization;

  Future<String?> authenticate() async {
    await (_initialization ??= _signIn.initialize(
      serverClientId: AppEnvironment.googleServerClientId,
    ));

    if (!_signIn.supportsAuthenticate()) {
      throw UnsupportedError(
        'Đăng nhập Google chỉ hỗ trợ trên ứng dụng Android/iOS hiện tại.',
      );
    }

    try {
      final account = await _signIn.authenticate();
      final idToken = account.authentication.idToken;
      if (idToken == null || idToken.isEmpty) {
        throw StateError('Google không trả về ID token. Vui lòng thử lại.');
      }
      return idToken;
    } on GoogleSignInException catch (error) {
      if (error.code == GoogleSignInExceptionCode.canceled) {
        return null;
      }
      throw StateError('Không thể đăng nhập Google: ${error.description}');
    }
  }

  Future<void> signOut() async {
    await _signIn.signOut();
  }
}
