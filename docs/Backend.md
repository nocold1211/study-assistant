# Study-Assistant Backend

Tapir( https://tapir.softwaremill.com/en/latest/ )가 여기서 핵심적인 라이브러리이다.

Tapir는 API를 타입으로 선언해서 컴파일 타임에 이를 검증할 수 있도록 되어 있다.

sttp로 클라이언트를 만들 수 있고, 그 타입은 tapir를 이용해 만들 수 있다.

common 모듈에서 tapir를 써서 api의 타입을 선언했고, 이를 backend와 frontend 양쪽에서 당겨 써서 구현했다. 이 방식을 쓰면, backend와 frontend의 api를 같은 타입이 되도록 강제할 수 있어서 양쪽의 정합성을 확보할 수 있다.

backend 안쪽에는 student 패키지와 openai 패키지가 있다.

## student 패키지

여기서는 config 파일을 읽은 다음 로그인 요청의 정합성을 검증해 access token을 발행한다.

## open ai 패키지

여기서는 Open AI API에 접근해서 채팅 응답을 만들어낸다

