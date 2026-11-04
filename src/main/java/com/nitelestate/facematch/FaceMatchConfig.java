package com.nitelestate.facematch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties(FaceMatchProperties.class)
public class FaceMatchConfig {

    /** Real AWS engine only when switched on; otherwise a no-op so the rest of the app is unaffected. */
    @Bean
    @ConditionalOnMissingBean(FaceEngine.class)
    public FaceEngine faceEngine(FaceMatchProperties props) {
        return props.enabled() ? new RekognitionFaceEngine(props) : new DisabledFaceEngine();
    }

    static class DisabledFaceEngine implements FaceEngine {
        public boolean isEnabled() { return false; }
        public String index(byte[] image, String externalId) { return null; }
        public void delete(List<String> faceIds) { }
        public List<Box> detect(byte[] image) { return List.of(); }
        public List<Hit> search(byte[] image, float minSimilarity, int maxResults) { return List.of(); }
    }
}
