FROM amazoncorretto:17

LABEL maintainer="development@bibliothek.uni-halle.de"

ARG VERSION
ARG JAR_FILE_NAME
ARG JAR_FILE

ENV APP_DIR="/app"
ENV DEST_JAR_FILE="$APP_DIR/$JAR_FILE_NAME"
ENV ENTRYPOINT_FILE="/entrypoint.sh"

RUN mkdir -p $APP_DIR

COPY $JAR_FILE $DEST_JAR_FILE

RUN echo -en "#!/bin/bash\njava -jar $DEST_JAR_FILE \"\$@\"\n" > "$ENTRYPOINT_FILE" \
    && chmod +x "$ENTRYPOINT_FILE"

ENTRYPOINT ["/entrypoint.sh"]
