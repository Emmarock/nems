package com.nitelestate.facematch;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.CreateCollectionRequest;
import software.amazon.awssdk.services.rekognition.model.DeleteFacesRequest;
import software.amazon.awssdk.services.rekognition.model.DescribeCollectionRequest;
import software.amazon.awssdk.services.rekognition.model.DetectFacesRequest;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.IndexFacesRequest;
import software.amazon.awssdk.services.rekognition.model.InvalidParameterException;
import software.amazon.awssdk.services.rekognition.model.QualityFilter;
import software.amazon.awssdk.services.rekognition.model.ResourceNotFoundException;
import software.amazon.awssdk.services.rekognition.model.SearchFacesByImageRequest;

import java.util.List;

/** AWS Rekognition-backed engine. Credentials come from the default AWS provider chain (env vars). */
@Slf4j
public class RekognitionFaceEngine implements FaceEngine {

    private final RekognitionClient client;
    private final String collectionId;
    private volatile boolean collectionReady = false;

    public RekognitionFaceEngine(FaceMatchProperties props) {
        this.client = RekognitionClient.builder().region(Region.of(props.region())).build();
        this.collectionId = props.collectionId();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String index(byte[] image, String externalId) {
        ensureCollection();
        var response = client.indexFaces(IndexFacesRequest.builder()
                .collectionId(collectionId)
                .image(image(image))
                .externalImageId(externalId)
                .maxFaces(1)
                .qualityFilter(QualityFilter.AUTO)
                .build());
        return response.faceRecords().isEmpty() ? null : response.faceRecords().get(0).face().faceId();
    }

    @Override
    public void delete(List<String> faceIds) {
        if (faceIds.isEmpty()) {
            return;
        }
        ensureCollection();
        client.deleteFaces(DeleteFacesRequest.builder().collectionId(collectionId).faceIds(faceIds).build());
    }

    @Override
    public List<Box> detect(byte[] image) {
        return client.detectFaces(DetectFacesRequest.builder().image(image(image)).build()).faceDetails().stream()
                .map(d -> new Box(d.boundingBox().left(), d.boundingBox().top(), d.boundingBox().width(), d.boundingBox().height()))
                .toList();
    }

    @Override
    public List<Hit> search(byte[] image, float minSimilarity, int maxResults) {
        ensureCollection();
        try {
            return client.searchFacesByImage(SearchFacesByImageRequest.builder()
                            .collectionId(collectionId)
                            .image(image(image))
                            .faceMatchThreshold(minSimilarity)
                            .maxFaces(maxResults)
                            // CCTV is often blurry; don't let the quality filter silently drop it.
                            .qualityFilter(QualityFilter.NONE)
                            .build())
                    .faceMatches().stream()
                    .map(m -> new Hit(m.face().faceId(), m.face().externalImageId(), m.similarity()))
                    .toList();
        } catch (InvalidParameterException e) {
            // Rekognition's answer when it can't find a face in the crop at all.
            return List.of();
        }
    }

    private void ensureCollection() {
        if (collectionReady) {
            return;
        }
        try {
            client.describeCollection(DescribeCollectionRequest.builder().collectionId(collectionId).build());
        } catch (ResourceNotFoundException e) {
            log.info("Creating Rekognition face collection {}", collectionId);
            client.createCollection(CreateCollectionRequest.builder().collectionId(collectionId).build());
        }
        collectionReady = true;
    }

    private static Image image(byte[] bytes) {
        return Image.builder().bytes(SdkBytes.fromByteArray(bytes)).build();
    }
}
