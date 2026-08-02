class TechTalkStrings {
  const TechTalkStrings(this.locale);
  final String locale;
  bool get en => locale == 'en';

  String get title => 'TechTalk AI';
  String get chooseScenario => en ? 'Choose a scenario' : 'Chọn tình huống';
  String get practiceScenarios =>
      en ? 'PRACTICE SCENARIOS' : 'CHỌN TÌNH HUỐNG LUYỆN TẬP';
  String get noScenarios => en
      ? 'No scenario is available. Add one in the Admin Panel.'
      : 'Chưa có scenario. Hãy thêm nội dung từ Admin Panel.';
  String get history => en ? 'History' : 'Lịch sử';
  String get transcript => en ? 'Chat transcript' : 'Đoạn chat';
  String get viewTranscript =>
      en ? 'View chat transcript' : 'Xem lại đoạn chat';
  String get readOnly => en ? 'Read-only' : 'Chỉ xem';
  String get transcriptDescription => en
      ? 'This is a saved copy of the conversation. You cannot send new messages here.'
      : 'Đây là bản lưu cuộc hội thoại. Bạn không thể gửi thêm tin nhắn tại đây.';
  String get emptyTranscript => en
      ? 'This session has no saved messages.'
      : 'Phiên này chưa có đoạn chat được lưu.';
  String get voiceMessage => en ? 'Voice message' : 'Tin nhắn giọng nói';
  String get mission => en ? 'Mission' : 'Nhiệm vụ';
  String get end => en ? 'End' : 'Kết thúc';
  String get abandon => en ? 'Abandon' : 'Rời phiên';
  String get continueSession => en ? 'Continue' : 'Tiếp tục';
  String get leaveQuestion => en
      ? 'Leave this roleplay session? You can abandon it or continue practicing.'
      : 'Rời phiên roleplay? Bạn có thể kết thúc phiên hoặc tiếp tục luyện tập.';
  String get inputHint =>
      en ? 'Type a Korean workplace message…' : 'Nhập câu tiếng Hàn…';
  String get hint => en ? 'Hint' : 'Gợi ý';
  String get recording =>
      en ? 'Recording… tap to stop' : 'Đang ghi âm… chạm để dừng';
  String get transcribing =>
      en ? 'Transcribing Korean audio…' : 'Đang nhận diện tiếng Hàn…';
  String get voiceTranscript => en
      ? 'Voice transcript — editable'
      : 'Transcript giọng nói — có thể chỉnh sửa';
  String get retry => en ? 'Retry' : 'Thử lại';
  String get evaluationUnavailable =>
      en ? 'Evaluation unavailable' : 'Chưa thể chấm lượt này';
  String get technicalError => en ? 'Diagnostic code' : 'Mã chẩn đoán';
  String get allObjectivesCompletedTitle =>
      en ? 'Mission completed' : 'Đã hoàn thành mục tiêu';
  String get allObjectivesCompletedMessage => en
      ? 'You have completed all mission objectives.'
      : 'Bạn đã hoàn thành tất cả mục tiêu.';
  String get viewResult => en ? 'View result' : 'Xem kết quả';
  String get continuePracticing =>
      en ? 'Continue practicing' : 'Tiếp tục luyện tập';
  String get corrections => en ? 'Corrections' : 'Chỉnh sửa đề xuất';
  String get result => en ? 'TechTalk Result' : 'Kết quả TechTalk';
  String get overall => en ? 'OVERALL' : 'ĐIỂM TỔNG';
  String get grammar => en ? 'Grammar' : 'Ngữ pháp';
  String get vocabulary => en ? 'Vocabulary' : 'Từ vựng';
  String get politeness => en ? 'Politeness' : 'Lịch sự';
  String get task => en ? 'Task' : 'Nhiệm vụ';
  String get improvements => en ? 'Improvements' : 'Cần cải thiện';
  String get objectives => en ? 'Objectives' : 'Mục tiêu';
  String get anotherScenario =>
      en ? 'Practice another scenario' : 'Luyện tình huống khác';
  String get noResult => en
      ? 'No roleplay result is available.'
      : 'Chưa có kết quả phiên roleplay.';
  String get completed => en ? 'Completed' : 'Hoàn thành';
  String get abandoned => en ? 'Abandoned' : 'Đã rời';
  String get active => en ? 'Active' : 'Đang học';
  String get resume => en ? 'Resume' : 'Tiếp tục';
  String get emptyHistory =>
      en ? 'No TechTalk sessions yet.' : 'Chưa có phiên TechTalk nào.';
  String get loadMore => en ? 'Load more' : 'Xem thêm';
  String get autoTts => en ? 'Auto-play AI voice' : 'Tự phát giọng AI';
  String get language => en ? 'Tiếng Việt' : 'English';
}
