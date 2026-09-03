# --- Build stage -------------------------------------------------------------------------
# One stage builds everything: frontend-maven-plugin (see pom.xml's `prod` profile) installs its
# own pinned Node/npm and runs `npm ci && npm run build`, so no separate Node base image is
# needed here - Maven drives the whole build.
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src/ src/
COPY frontend/ frontend/

RUN ./mvnw -B -ntp clean package -Pprod -DskipTests

# --- Runtime stage -------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN useradd --system --create-home --shell /usr/sbin/nologin nitel
COPY --from=build /app/target/nitel-estate.jar app.jar
RUN chown nitel:nitel app.jar
USER nitel

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
