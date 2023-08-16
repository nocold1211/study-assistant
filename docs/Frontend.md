# Study-Assistant Frontend

스칼라JS 쪽에서는 Tyrian ( https://tyrian.indigoengine.io/ ) 라이브러리가 핵심적으로 사용되고 있다.

StudyAssistantApp 파일에 대부분 정의되어 있다.

자세한 내용은 가이드 ( https://tyrian.indigoengine.io/guides/guided-tour/ )를 참조하면 알 수 있다. 대략적인 개괄은 다음과 같다

* 모델: 웹페이지 상에서 변동하는 상태를 표현하고 있다. Study-Assistant에서는 ChatAppModel이 여기에 해당한다.
  * ChatAppModel
    * LoginModel과 ChatModel 둘 중 하나가 된다
    * LoginModel
      * 로그인 창이 떠 있는 상황에서의 상태
      * username, password 두 가지 데이터를 가지고 있다.
    * ChatModel
      * 채팅창이 떠 있는 상황에서의 상태
      * 로그인후 API 서버에서 받은 access key, 채팅 인풋 창, 채팅 기록, 로딩중 여부 의 데이터를 갖고 있다
* 뷰: 모델을 기반으로 웹 페이지를 그리는 방법
  * 대강의 문법은 https://tyrian.indigoengine.io/guides/html/ 문서를 참조.
  * 스칼라 코드로 정의해놓으면 이를 기반으로 html을 만들어준다
  * LoginModel인 경우, ChatModel인 경우 각각에 대해서 웹페이지를 정의해놓았다.
* 메세지: 모델이 변경되어야 할 필요가 생겼음을 알리는 자료구조. Study-Assistant에서는 ChatAppMsg 를 쓴다
  * 주로 사용자가 어떤 입력을 하거나, 네트워크에서 어떤 응답을 받을 때 모델 업데이트가 필요해진다.
  * ChatAppMsg: 다음 경우들 중 하나이다
    * NoOp: 아무 일이 일어나지 않도 됨을 표현한다
    * ErrorMsg: 어떤 에러 메세지를 표시해야 함을 나타낸다
    * StartChat: 로그인이 성공했고, 채팅을 시작해야 함을 나타낸다
    * UpdateChatInput: 채팅 인풋창에 업데이트가 필요함을 나타낸다.
    * SendChat: 채팅 응답을 받기 위해 API 서버에 현재 채팅 기록을 보내야 함을 나타낸다.
    * ReceiveChat: 채팅 응답을 받았음을 나타낸다
    * LoginMsg: 다음 세 가지 케이스인다
      * UpdateUsername 사용자 이름이 업데이트되었음을 나타낸다
      * UpdatePassword 비밀번호가 업데이트되었음을 나타낸다
      * Submit 사용자 이름과 비밀번호를 제출해 로그인 시도를 함을 나타낸다
* 업데이트: 메세지를 받아서 모델을 업데이트하고 특정한 액션을 수행하는 것을 나타낸다





