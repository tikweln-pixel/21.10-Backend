#!/bin/bash
# Arranca el backend con credenciales Supabase pasadas como propiedades JVM
# (prioridad maxima, anula cualquier variable de entorno del sistema)
export MAVEN_OPTS="-Djava.net.preferIPv4Stack=true"
mvn spring-boot:run \
  -Dspring.datasource.url="jdbc:postgresql://aws-1-eu-west-1.pooler.supabase.com:6543/postgres?sslmode=require" \
  -Dspring.datasource.username="postgres.bmulgijtddwdwaajktay" \
  -Dspring.datasource.password="6zlvQBAIoK4gdNQB"
