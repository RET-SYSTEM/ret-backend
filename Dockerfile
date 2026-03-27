# ==========================================
# STAGE 1: Build the application
# ==========================================
# We use a heavy Maven image just to compile your code.
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy the pom.xml and source code into the container
COPY pom.xml .
COPY src ./src

# Tell Maven to compile the cde into a .jar file (skipping tests to be fast)
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: Run the application
# ==========================================
# We switch to a tiny JRE (Java Runtime) image.
# We don't need the heavy Maven stuff anymore, just enough to run the app.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy ONLY the compiled .jar file from STAGE 1 into this new, clean box
COPY --from=build /app/target/*.jar app.jar

# Expose port 8080 so the outside world (or the VPS) can talk to it
EXPOSE 8080

# The command that runs when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]