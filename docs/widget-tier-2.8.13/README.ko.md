# Android 네이티브 위젯 렌더링 참고

Android 15(API35) 격리 에뮬레이터의 실제 RemoteViews/XML/GradientDrawable/원본 휘장/WidgetGraphics로 렌더링했습니다. 사용률 35%, 초기화 4일 13시간은 합성 표시값이며 실제 계정 관측이 아닙니다. 배경 불투명도는 100%입니다. 삼성 One UI Home에 배치한 화면이나 물리 기기 캡처가 아닙니다.

110×70dp 및 250×70dp 크기를 검증했습니다. 기기에서 제공되는 실제 위젯 크기·글꼴·배경에 따라 표시 크기는 달라집니다. `WidgetTierNativeTest` 및 `verify-widget-tier-native.py`로 빈 테스트 에뮬레이터에서 재현할 수 있습니다. 이미 설치된 앱이 있는 에뮬레이터는 자동 실행 도구가 거절합니다.
