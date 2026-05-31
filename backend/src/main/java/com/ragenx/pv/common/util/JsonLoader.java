package com.ragenx.pv.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragenx.pv.common.error.ApiException;
import com.ragenx.pv.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads a classpath JSON resource into a typed object using the Spring-configured
 * {@link ObjectMapper} (so snake_case keys map correctly). Fails loud: a missing or
 * unparseable resource throws, which aborts startup with a named cause rather than
 * silently booting with no data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonLoader {

    private final ObjectMapper objectMapper;

    public <T> T load(String classpathResource, Class<T> type) {
        ClassPathResource resource = new ClassPathResource(classpathResource);
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, type);
        } catch (IOException e) {
            log.error("json.load.failed resource={}", classpathResource, e);
            throw new ApiException(ErrorCode.SYSTEM_UNEXPECTED,
                    "Failed to load required resource: " + classpathResource);
        }
    }
}
