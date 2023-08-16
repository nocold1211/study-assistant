# Study-Assistant

## Dev

### Backend

> sbt backend/run

continuous mode(소스 코드 변경 즉시 재컴파일 및 재실행)

> sbt ~backend/reStart

### Frontend ScalaJS

> sbt frontend/fastLinkJS

continuous mode(소스 코드 변경 즉시 재컴파일 및 재실행)

> sbt ~backend/fastLinkJS

### Frontend Web

> cd modules/frontend
>
> yarn start

## Production

### Backend

> sbt backend/assembly

이후 fat jar 파일이 생성되면 scp로 서버에 복사하고 실행

### Frontend

master에 커밋 되면 cloudfront 빌드 서버에서 알아서 deploy.sh를 실행해 자동 배포

## Project Structure

자세한 문서는 docs 폴더 안에 남길 예정.

### Backend

* 위치: modules/backend
* API 서버. 요청이 들어오면 OpenAI API를 써서 전달하고 응답을 가공해서 돌려준다

### Frontend

* 위치: modules/frontend

* Scala JS를 써서 자바스크립트 파일로 컴파일하고, modules/frontend/assets 폴더 안쪽의 다른 html, css 파일들을 엮어서 웹페이지를 만들고 서방한다.

### Common

* 위치: modules/common
* backend와 frontend에서 공유하는 scala 코드들. 양쪽에서 다 사용한다. 주로 API 규격 정의가 들어 있다.







