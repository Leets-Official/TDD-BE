# 런타임 전용 이미지. 빌드는 GitHub Actions에서 수행하고, 이 이미지는 산출물만 실행한다.
# 빌드 컨텍스트에 app.jar가 있어야 한다(배포 워크플로우가 bootJar 결과를 그 이름으로 넣는다).
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
